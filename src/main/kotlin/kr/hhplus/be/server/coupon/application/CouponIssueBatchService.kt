package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.IssuedStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CouponIssueBatchService(
    private val couponKVStore: CouponKVStore,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processIssueRequest() {
        logger.info("[processIssueRequest] start")
        // 발급 요청된 쿠폰 ID 리스트에서 하나 꺼내기 (원자적 연산)
        val couponId = couponKVStore.popFromIssueRequestedCouponIdList() ?: return
        
        try {
            // 해당 쿠폰의 최대 발급 수량 확인
            val stock = couponKVStore.getStock(couponId)
            logger.info("[processIssueRequest] stock: $stock")
            // 현재까지 발급된 수량 확인
            val issuedCount = couponKVStore.countIssuedUser(couponId)
            logger.info("[processIssueRequest] issuedCount: $issuedCount")
            // 발급 가능한 수량 계산
            val availableCount = stock.stock - issuedCount
            logger.info("[processIssueRequest] availableCount: $availableCount")
            
            if (availableCount <= 0) {
                // 재고가 부족한 경우 - 쿠폰 ID는 나중에 별도 프로세스에서 처리
                couponKVStore.pushToOutOfStockCouponIdList(couponId)
                return
            }
            
            // 발급 가능한 수량만큼 큐에서 요청 꺼내기
            val requests = couponKVStore.popBatchFromIssueRequestQueue(couponId, availableCount)
            logger.info("[processIssueRequest] requests: ${requests.size}")
            if (requests.isEmpty()) {
                return
            }
            
            // 처리할 요청들을 PROCESSING 상태로 설정
            requests.forEach { request ->
                couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.PROCESSING)
            }
            
            // 쿠폰 도메인 객체 조회
            val coupon = couponRepository.getByIdForUpdate(couponId)
            val now = LocalDateTime.now()
            
            // 중복 발급 여부 검사 및 발급 처리
            val validRequestsMap = requests.filter { request ->
                val result = !couponKVStore.existsIssuedUser(request.userId, request.couponId)
                if (!result) {
                    // 중복 검증 실패 시 FAILED로 상태 변경
                    couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
                }
                result
            }.associateBy { it.userId }
            
            // 유효한 요청에 대해 UserCoupon 객체 생성
            val userCoupons = validRequestsMap.values.map { request ->
                coupon.asyncIssueTo(userId = request.userId, now = now)
            }

            if(userCoupons.isEmpty()) {
                return
            }

            val validUserCoupons = userCoupons.filter { userCoupon ->
                runCatching {
                    val result = couponKVStore.markAsIssued(userCoupon.userId, couponId)
                    return@filter result
                }
                    .onFailure {
                        couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userCoupon.userId]!!)
                        couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
                    }.isSuccess
            }

            logger.info("[processIssueRequest] validUserCoupons: ${validUserCoupons.size}")

            validUserCoupons.forEach { userCoupon ->
                runCatching { userCouponRepository.save(userCoupon) }
                    .onFailure { e ->
                        userCoupons.forEach { couponKVStore.rollbackIssuedMark(it.userId, couponId) }
                        couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userCoupon.userId]!!)
                        couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
                    }
            }

            logger.info("[processIssueRequest] processed requests: ${requests.size}")
            // 현재 큐에 남은 요청 수 확인
            val remainingRequests = couponKVStore.countIssueRequestQueue(couponId)
            
            // 요청이 남아있고 재고가 모두 소진되었으면 OutOfStock 처리
            if (remainingRequests > 0 && (stock.stock - issuedCount - requests.size) <= 0) {
                couponKVStore.pushToOutOfStockCouponIdList(couponId)
            }
        } catch (e: Exception) {
            // 예외 발생 시 쿠폰 ID를 다시 리스트에 추가하여 재처리 가능하도록 함
            couponKVStore.pushToIssueRequestedCouponIdList(couponId)
            throw e
        }
    }
    
    @Transactional
    fun processFailedIssueRequest() {
        // 실패한 발급 요청 쿠폰 ID 리스트에서 하나 꺼내기 (원자적 연산)
        val couponId = couponKVStore.popFromFailedIssueRequestedCouponIdList() ?: return
        
        try {
            // 실패한 요청 큐에서 배치로 데이터 꺼내기 (최대 100개)
            val failedRequests = couponKVStore.popBatchFromFailedIssueRequestQueue(couponId, 100)
            if (failedRequests.isEmpty()) {
                return
            }
            
            // 처리할 요청들을 PROCESSING 상태로 설정
            failedRequests.forEach { request ->
                couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.PROCESSING)
            }
            
            // 쿠폰 도메인 객체 조회
            val coupon = couponRepository.getByIdForUpdate(couponId)
            val now = LocalDateTime.now()
            
            // 중복 발급 여부 검사 및 발급 처리
            val validRequestsMap = failedRequests.filter { request ->
                val result = !couponKVStore.existsIssuedUser(request.userId, request.couponId)
                if (!result) {
                    // 중복 검증 실패 시 FAILED로 상태 변경
                    couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
                }
                result
            }.associateBy { it.userId }
            
            // 유효한 요청에 대해 UserCoupon 객체 생성
            val userCoupons = validRequestsMap.values.map { request ->
                coupon.issueTo(request.userId, now)
            }

            if (userCoupons.isEmpty()) {
                return
            }

            val validUserCoupons = userCoupons.filter { userCoupon ->
                runCatching {
                    val result = couponKVStore.markAsIssued(userCoupon.userId, couponId)
                    return@filter result
                }
                    .onFailure {
                        couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userCoupon.userId]!!)
                        couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
                    }.isSuccess
            }

            validUserCoupons.forEach { userCoupon ->
                runCatching { userCouponRepository.save(userCoupon) }
                    .onFailure { e ->
                        userCoupons.forEach { couponKVStore.rollbackIssuedMark(it.userId, couponId) }
                        couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userCoupon.userId]!!)
                        couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
                    }
            }
            
            // 아직 처리하지 못한 실패 요청이 남아있는 경우
            if (couponKVStore.countFailedIssueRequestQueue(couponId) > 0) {
                couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
            }
        } catch (e: Exception) {
            // 예외 발생 시 쿠폰 ID를 다시 리스트에 추가하여 재처리 가능하도록 함
            couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
            throw e
        }
    }
    
    @Transactional
    fun processOutOfStockRequests(batchSize: Long = 100) {
        // 재고 부족으로 처리되지 못한 쿠폰 ID 리스트에서 하나 꺼내기 (원자적 연산)
        val couponId = couponKVStore.popFromOutOfStockCouponIdList() ?: return
        
        try {
            // 일정 배치 크기만큼 요청 큐에서 데이터 꺼내기
            val requests = couponKVStore.peekBatchFromIssueRequestQueue(couponId, batchSize)
            if (requests.isEmpty()) {
                return
            }
            
            // 요청들을 FAILED 상태로 변경
            requests.forEach { request ->
                couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
            }
            
            // 아직 처리하지 못한 요청이 남아있는 경우
            if (requests.size.toLong() >= batchSize && couponKVStore.countIssueRequestQueue(couponId) > batchSize) {
                couponKVStore.pushToOutOfStockCouponIdList(couponId)
            }
        } catch (e: Exception) {
            // 예외 발생 시 쿠폰 ID를 다시 리스트에 추가하여 재처리 가능하도록 함
            couponKVStore.pushToOutOfStockCouponIdList(couponId)
            throw e
        }
    }
} 