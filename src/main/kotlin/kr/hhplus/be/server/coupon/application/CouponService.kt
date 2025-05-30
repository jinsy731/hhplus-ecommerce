package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.application.dto.CouponResult
import kr.hhplus.be.server.coupon.application.dto.toDiscountContext
import kr.hhplus.be.server.coupon.application.mapper.CouponMapper
import kr.hhplus.be.server.coupon.application.port.CouponApplicationService
import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.coupon.domain.CouponIssueRequestedPayload
import kr.hhplus.be.server.coupon.domain.port.CouponEventPublisher
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService
import kr.hhplus.be.server.coupon.infrastructure.kvstore.*
import kr.hhplus.be.server.lock.annotation.WithDistributedLock
import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.shared.exception.CouponOutOfStockException
import kr.hhplus.be.server.shared.exception.DuplicateCouponIssueException
import kr.hhplus.be.server.shared.time.ClockHolder
import kr.hhplus.be.server.shared.web.PageResult
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Pageable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val discountLineRepository: DiscountLineRepository,
    private val couponKVStore: CouponKVStore,
    private val couponEventPublisher: CouponEventPublisher,
    private val clockHolder: ClockHolder,
    private val couponMapper: CouponMapper,
    private val couponDomainService: CouponDomainService
    ) : CouponApplicationService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @WithDistributedLock(
        key = "'coupon:' + #cmd.couponId",
        type = LockType.PUBSUB
    )
    @Transactional
    override fun issueCoupon(cmd: CouponCommand.Issue): CouponResult.Issue {
        userCouponRepository.findByUserIdAndCouponId(cmd.userId, cmd.couponId)
            ?.let { throw DuplicateCouponIssueException() }

        val coupon = couponRepository.getByIdForUpdate(cmd.couponId)
        val userCoupon = coupon.issueTo(cmd.userId, clockHolder.getNowInLocalDateTime())

        val savedUserCoupon = userCouponRepository.save(userCoupon)

        return CouponResult.Issue(
            userCouponId = savedUserCoupon.id,
            status = savedUserCoupon.status,
            expiredAt = savedUserCoupon.expiredAt
        )
    }

    @Transactional
    override fun issueCouponAsync(cmd: CouponCommand.Issue): CouponResult.AsyncIssue {
        // 1. 중복 검증
        if (couponKVStore.existsIssuedUser(cmd.userId, cmd.couponId)) {
            throw DuplicateCouponIssueException()
        }

        // 2. 쿠폰 stock 및 발급 가능 여부 검증
        if(couponKVStore.getStock(cmd.couponId).stock <= 0) {
            couponKVStore.setStock(CouponStock(cmd.couponId, couponRepository.getById(cmd.couponId).maxIssueLimit.toLong()))
        }
        val stock = couponKVStore.getStock(cmd.couponId)
        val issuedCount = couponKVStore.countIssuedUser(cmd.couponId)
        
        if (stock.stock <= issuedCount) {
            throw CouponOutOfStockException()
        }

        // 3. 처리중 상태로 설정
        couponKVStore.setIssuedStatus(cmd.userId, cmd.couponId, IssuedStatus.PENDING)
        
        // 4. 쿠폰 발급 요청 큐에 삽입
        val issueRequest = CouponIssueRequest(cmd.couponId, cmd.userId)
        couponKVStore.pushToIssueReqeustQueue(issueRequest)
        logger.info("[issueCouponAsync] pushed to issue request queue: $issueRequest")
        
        // 5. 요청 처리를 위해 쿠폰 ID 리스트에 추가
        couponKVStore.pushToIssueRequestedCouponIdList(cmd.couponId)
        
        // 6. 처리중 응답 반환
        return CouponResult.AsyncIssue(
            couponId = cmd.couponId,
            status = IssuedStatus.PENDING.name
        )
    }

    /**
     * Redis + Kafka를 활용한 개선된 비동기 쿠폰 발급
     * Lua Script를 활용하여 원자적으로 검증 및 처리
     */
    @Transactional
    fun issueCouponWithRedisKafka(cmd: CouponCommand.Issue): CouponResult.AsyncIssue {
        logger.info("[issueCouponWithRedisKafka] 쿠폰 발급 요청 시작: userId=${cmd.userId}, couponId=${cmd.couponId}")
        
        try {
            if(couponKVStore.getStock(cmd.couponId).stock == 0L) {
                couponKVStore.setStock(CouponStock(cmd.couponId, couponRepository.getById(cmd.couponId).maxIssueLimit.toLong()))
            }
            // 1. Redis Lua Script를 활용한 사전 검증 및 처리
            val validationResult = couponKVStore.validateAndMarkCouponIssue(cmd.userId, cmd.couponId)
            
            if (!validationResult.isValid) {
                logger.warn("[issueCouponWithRedisKafka] 검증 실패: ${validationResult.errorMessage}")
                
                val exception = when (validationResult.errorCode) {
                    CouponIssueValidationResult.ERROR_DUPLICATE_ISSUE -> DuplicateCouponIssueException()
                    CouponIssueValidationResult.ERROR_OUT_OF_STOCK -> CouponOutOfStockException()
                    else -> RuntimeException(validationResult.errorMessage ?: "쿠폰 발급 실패")
                }
                throw exception
            }
            
            // 2. 검증 성공 시 PENDING 상태로 설정
            couponKVStore.setIssuedStatus(cmd.userId, cmd.couponId, IssuedStatus.PENDING)
            logger.info("[issueCouponWithRedisKafka] 발급 상태를 PENDING으로 설정: userId=${cmd.userId}, couponId=${cmd.couponId}")
            
            // 3. Application Event 발행
            val issuedAt = clockHolder.getNowInLocalDateTime()
            val event = CouponEvent.IssueRequested(
                payload = CouponIssueRequestedPayload(
                    userId = cmd.userId,
                    couponId = cmd.couponId,
                    issuedAt = issuedAt
                )
            )
            
            try {
                // Spring ApplicationEvent를 통한 이벤트 발행 (동기적)
                couponEventPublisher.publishIssueRequested(event)
                logger.info("[issueCouponWithRedisKafka] 애플리케이션 이벤트 발행 성공: $event")
                
                return CouponResult.AsyncIssue(
                    couponId = cmd.couponId,
                    status = "REQUESTED"
                )
            } catch (e: Exception) {
                logger.error("[issueCouponWithRedisKafka] 이벤트 발행 실패, 보상 처리 시작", e)
                
                // 4. 이벤트 발행 실패 시 보상 로직 수행
                val rollbackSuccess = couponKVStore.rollbackCouponIssue(cmd.userId, cmd.couponId)
                if (!rollbackSuccess) {
                    logger.error("[issueCouponWithRedisKafka] 보상 처리도 실패: userId=${cmd.userId}, couponId=${cmd.couponId}")
                } else {
                    logger.info("[issueCouponWithRedisKafka] 보상 처리 성공: userId=${cmd.userId}, couponId=${cmd.couponId}")
                }
                
                throw RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다.", e)
            }
            
        } catch (e: Exception) {
            logger.error("[issueCouponWithRedisKafka] 쿠폰 발급 실패: userId=${cmd.userId}, couponId=${cmd.couponId}", e)
            throw e
        }
    }

    /**
     * 비동기 쿠폰 발급 상태 조회
     * KVStore에서 발급 상태를 확인하고 결과를 반환
     */
    @Transactional(readOnly = true)
    fun getIssueStatus(userId: Long, couponId: Long): CouponResult.AsyncIssueStatus {
        // KVStore에서 발급 상태 조회
        val status = couponKVStore.getIssuedStatus(userId, couponId)
        
        // 발급 완료 상태인 경우 DB에서 유저 쿠폰 ID 조회
        val userCouponId = if (status == IssuedStatus.ISSUED) {
            userCouponRepository.findByUserIdAndCouponId(userId, couponId)?.id
        } else {
            null
        }
        
        return CouponResult.AsyncIssueStatus(
            couponId = couponId,
            status = status.name,
            userCouponId = userCouponId
        )
    }

    /**
     * 쿠폰 적용 메서드
     * 1. 할인 적용 대상 필터링
     * 2. 적용 대상 전체에 대한 할인 금액 계산
     * 3. 각 대상에 할인 금액 분배 (물품별 할인 금액 계산을 위해)
     */
    @WithMultiDistributedLock(
        keys = ["#cmd.userCouponIds.![ 'user:coupon:' + #this ]"],
        type = LockType.SPIN
    )
    @Transactional
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 300, multiplier = 1.5)
    )
    override fun use(cmd: CouponCommand.Use.Root): Result<CouponResult.Use> {
        return runCatching {
            val userCoupons = userCouponRepository.findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)
            val discountContext = cmd.toDiscountContext()
            
            val discountLines = userCoupons.flatMap { userCoupon ->
                couponDomainService.calculateDiscountAndUse(userCoupon, discountContext, cmd.orderId)
            }

            val savedDiscountLine = discountLineRepository.saveAll(discountLines)
            val discountInfo = couponMapper.mapToDiscountInfoList(savedDiscountLine)

            CouponResult.Use(discountInfo = discountInfo)
        }
    }

    @Transactional
    override fun restoreCoupons(orderId: Long, orderEventPayload: OrderEventPayload) {
        // orderId로 사용된 쿠폰들을 조회
        val userCoupons = userCouponRepository.findAllByOrderId(orderId)

        userCoupons.forEach { userCoupon -> userCoupon.restore() }

        userCouponRepository.saveAll(userCoupons)
    }

    @Transactional(readOnly = true)
    override fun retrieveLists(userId: Long, pageable: Pageable): CouponResult.RetrieveList {
        val userCouponPage = userCouponRepository.findAllByUserId(userId, pageable)

        // 각 UserCoupon에 대한 Coupon 정보를 미리 조회
        val couponIds = userCouponPage.content.map { it.couponId }.distinct()
        val couponsMap = couponIds.associateWith { couponId ->
            couponDomainService.getCouponInfo(couponId)
        }

        return CouponResult.RetrieveList(
            coupons = userCouponPage.content.map { uc -> 
                couponMapper.mapToUserCouponData(uc, couponsMap[uc.couponId]!!) 
            },
            pageResult = PageResult.of(userCouponPage)
        )
    }

    @Transactional(readOnly = true)
    override fun getUsedCouponIdsByOrderId(orderId: Long): List<Long> {
        val userCoupons = userCouponRepository.findAllByOrderId(orderId)
        return userCoupons.map { it.id!! }
    }
}
