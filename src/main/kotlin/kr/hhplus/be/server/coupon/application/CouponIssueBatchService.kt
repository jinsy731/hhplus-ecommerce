package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.CouponIssueRequest
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
    private val BATCH_SIZE = 1000L

    @Transactional
    fun processIssueRequest() {
        logger.info("[processIssueRequest] 시작")
        val couponId = couponKVStore.peekFromIssueRequestedCouponIdList() ?: return

        try {
            val stock = couponKVStore.getStock(couponId)
            val issuedCount = couponKVStore.countIssuedUser(couponId)
            val availableCount = stock.stock - issuedCount
            
            logger.info("[processIssueRequest] 최대 발급 수량: {}, 발급수량: {}, 가용수량: {}", stock.stock, issuedCount, availableCount)

            val requests = couponKVStore.peekBatchFromIssueRequestQueue(couponId, BATCH_SIZE)

            val (validRequests, outOfStockRequests) = splitRequestsByAvailability(requests, availableCount)
            markOutOfStockRequests(outOfStockRequests)

            processValidRequests(couponId, validRequests)
            
            couponKVStore.popBatchFromIssueRequestQueue(couponId, requests.size.toLong())
            couponKVStore.popFromIssueRequestedCouponIdList()

        } catch (e: Exception) {
            handleException(couponId, e)
        }
    }

    @Transactional
    fun processFailedIssueRequest() {
        val couponId = couponKVStore.peekFromFailedIssueRequestedCouponIdList() ?: return
        
        try {
            val failedRequests = couponKVStore.popBatchFromFailedIssueRequestQueue(couponId, 100)

            val coupon = couponRepository.getByIdForUpdate(couponId)
            val now = LocalDateTime.now()

            val requestMap = failedRequests.associateBy { it.userId }
            val userCoupons = requestMap.map { (userId, requests) ->
                coupon.asyncIssueTo(userId, now)
            }

            processUserCoupons(couponId, userCoupons, requestMap)

            couponKVStore.popBatchFromFailedIssueRequestQueue(couponId, failedRequests.size.toLong())
            couponKVStore.popFromFailedIssueRequestedCouponIdList()
        } catch (e: Exception) {
            handleException(couponId, e)
        }
    }

    private fun splitRequestsByAvailability(requests: List<CouponIssueRequest>, availableCount: Long): Pair<List<CouponIssueRequest>, List<CouponIssueRequest>> {
        val validRequests = requests.take(availableCount.toInt())
        val outOfStockRequests = requests.drop(availableCount.toInt())
        return Pair(validRequests, outOfStockRequests)
    }

    private fun markOutOfStockRequests(requests: List<CouponIssueRequest>) {
        requests.forEach { request ->
            couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
        }
    }

    private fun processValidRequests(couponId: Long, requests: List<CouponIssueRequest>) {
        val coupon = couponRepository.getByIdForUpdate(couponId)
        val validRequestsMap = filterValidRequests(requests)
        val userCoupons = createUserCoupons(coupon, validRequestsMap, LocalDateTime.now())
        processUserCoupons(couponId, userCoupons, validRequestsMap)
    }

    private fun filterValidRequests(requests: List<CouponIssueRequest>): Map<Long, CouponIssueRequest> {
        return requests.filter { request ->
            val result = !couponKVStore.existsIssuedUser(request.userId, request.couponId)
            if (!result) {
                couponKVStore.setIssuedStatus(request.userId, request.couponId, IssuedStatus.FAILED)
            }
            result
        }.associateBy { it.userId }
    }

    private fun createUserCoupons(coupon: Coupon, validRequestsMap: Map<Long, CouponIssueRequest>, now: LocalDateTime): List<UserCoupon> {
        return validRequestsMap.values.map { request ->
            coupon.asyncIssueTo(request.userId, now)
        }
    }

    private fun processUserCoupons(couponId: Long, userCoupons: List<UserCoupon>, validRequestsMap: Map<Long, CouponIssueRequest>) {
        val validUserCoupons = userCoupons.filter { userCoupon ->
            markAsIssued(userCoupon.userId, couponId, validRequestsMap)
        }

        validUserCoupons.forEach { userCoupon ->
            saveUserCoupon(userCoupon, userCoupons, couponId, validRequestsMap)
        }
    }

    private fun markAsIssued(userId: Long, couponId: Long, validRequestsMap: Map<Long, CouponIssueRequest>): Boolean {
        return runCatching {
            couponKVStore.markAsIssued(userId, couponId)
        }.onFailure {
            couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userId]!!)
            couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
        }.isSuccess
    }

    private fun saveUserCoupon(userCoupon: UserCoupon, allUserCoupons: List<UserCoupon>, couponId: Long, validRequestsMap: Map<Long, CouponIssueRequest>) {
        runCatching { 
            userCouponRepository.save(userCoupon) 
        }.onFailure { e ->
            // DB Insert에 실패해도 상태를 롤백하지 않고 그대로 가져감.
            // 상태를 롤백하고 실패 요청을 재시도 하는 경우 요청 순서가 보장되지 않을 수 있으므로, 성공으로 마킹하고 재시도를 통해 최종 일관성을 보장함.
            couponKVStore.pushToFailedIssueRequestQueue(validRequestsMap[userCoupon.userId]!!)
            couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)
        }
    }

    private fun handleException(couponId: Long, e: Exception) {
        couponKVStore.pushToIssueRequestedCouponIdList(couponId)
        throw e
    }
} 