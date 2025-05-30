package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.application.dto.CouponResult
import kr.hhplus.be.server.coupon.application.dto.toDiscountContext
import kr.hhplus.be.server.coupon.application.mapper.CouponMapper
import kr.hhplus.be.server.coupon.application.port.CouponApplicationService
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponIssueRequest
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
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
    private val clockHolder: ClockHolder,
    private val couponMapper: CouponMapper
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
            val discountLines = userCouponRepository
                .findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)
                .flatMap { it.calculateDiscountAndUse(cmd.toDiscountContext(), cmd.orderId) }

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

        return CouponResult.RetrieveList(
            coupons = userCouponPage.content.map { uc -> couponMapper.mapToUserCouponData(uc) },
            pageResult = PageResult.of(userCouponPage)
        )
    }

    @Transactional(readOnly = true)
    override fun getUsedCouponIdsByOrderId(orderId: Long): List<Long> {
        val userCoupons = userCouponRepository.findAllByOrderId(orderId)
        return userCoupons.map { it.id!! }
    }
}
