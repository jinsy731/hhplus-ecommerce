package kr.hhplus.be.server.coupon.application

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

    val BATCH_SIZE = 1000L

    @Transactional
    fun processIssueRequest() {
        logger.info("[processIssueRequest] start")
        // 발급 요청된 쿠폰 ID 리스트에서 하나 꺼내기 (원자적 연산)
        val couponId = couponKVStore.peekFromIssueRequestedCouponIdList() ?: return // pop 하지 않고 peek 만하고 처리가 완료되면 pop
        
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

            
            // 배치 사이즈만큼 꺼내고, 발급 수량 초과분에 대해서는 FAILED 마킹
            val requests = couponKVStore.peekBatchFromIssueRequestQueue(couponId, BATCH_SIZE) // pop 하지 않고 peek 만하고 처리가 완료되면 pop
            logger.info("[processIssueRequest] requests: ${requests.size}")
            if (requests.isEmpty()) return

            val validRequest = requests.take(availableCount.toInt())
            val outOfStockRequest = requests.drop(availableCount.toInt())

            outOfStockRequest.forEach { request ->
                couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
            }

            if(validRequest.isEmpty()) return

            // 쿠폰 도메인 객체 조회
            val coupon = couponRepository.getByIdForUpdate(couponId)

            // 중복 발급 여부 검사 및 발급 처리
            val validRequestsMap = validRequest.filter { request ->
                val result = !couponKVStore.existsIssuedUser(request.userId, request.couponId)
                if (!result) {
                    // 중복 검증 실패 시 FAILED로 상태 변경
                    couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
                }
                result
            }.associateBy { it.userId }
            
            // 유효한 요청에 대해 UserCoupon 객체 생성
            val userCoupons = validRequestsMap.values.map { request ->
                coupon.asyncIssueTo(userId = request.userId, now = LocalDateTime.now())
            }

            val validUserCoupons = userCoupons.filter { userCoupon ->
                runCatching {
                    couponKVStore.markAsIssued(userCoupon.userId, couponId)
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

            couponKVStore.popFromIssueRequestedCouponIdList()
            couponKVStore.popBatchFromIssueRequestQueue(couponId, requests.size.toLong())

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

            couponKVStore.popBatchFromIssueRequestQueue(couponId, batchSize)
            
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