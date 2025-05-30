package kr.hhplus.be.server.coupon.infrastructure.kvstore

interface CouponKVStore {
    fun existsIssuedUser(userId: Long, couponId: Long): Boolean
    fun setIssuedUser(userId: Long, couponId: Long)
    fun countIssuedUser(couponId: Long): Long

    fun pushToIssueReqeustQueue(issueRequest: CouponIssueRequest)
    fun popBatchFromIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>
    fun peekAllFromIssueRequestQueue(couponId: Long): List<CouponIssueRequest>
    fun peekBatchFromIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>
    fun countIssueRequestQueue(couponId: Long): Long

    fun pushToFailedIssueRequestQueue(issueRequest: CouponIssueRequest)
    fun pushAllToFailedIssueRequestQueue(failedRequests: List<CouponIssueRequest>)
    fun peekBatchFromFailedIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>
    fun popBatchFromFailedIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>
    fun countFailedIssueRequestQueue(couponId: Long): Long

    fun getStock(couponId: Long): CouponStock
    fun getStocks(couponIds: List<Long>): CouponStock
    fun setStock(couponStock: CouponStock)

    fun setIssuedStatus(userId: Long, couponId: Long, status: IssuedStatus)
    fun getIssuedStatus(userId: Long, couponId: Long): IssuedStatus

    fun pushToIssueRequestedCouponIdList(couponId: Long)
    fun popFromIssueRequestedCouponIdList(): Long?
    fun peekFromIssueRequestedCouponIdList(): Long?
    
    fun pushToFailedIssueRequestedCouponIdList(couponId: Long)
    fun popFromFailedIssueRequestedCouponIdList(): Long?
    fun peekFromFailedIssueRequestedCouponIdList(): Long?
    
    fun pushToOutOfStockCouponIdList(couponId: Long)
    fun popFromOutOfStockCouponIdList(): Long?

    fun markAsIssued(userId: Long, couponId: Long): Boolean
    fun rollbackIssuedMark(userId: Long, couponId: Long): Boolean
    
    // Redis + Kafka 개선된 발급을 위한 메서드들
    /**
     * Lua Script를 활용한 원자적 쿠폰 발급 사전 검증 및 처리
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return CouponIssueValidationResult 검증 결과
     */
    fun validateAndMarkCouponIssue(userId: Long, couponId: Long): CouponIssueValidationResult
    
    /**
     * 쿠폰 발급 실패 시 롤백 처리 (보상 로직)
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return Boolean 롤백 성공 여부
     */
    fun rollbackCouponIssue(userId: Long, couponId: Long): Boolean
}

data class CouponIssueRequest(
    val couponId: Long,
    val userId: Long
)

data class CouponStock(
    val couponId: Long,
    val stock: Long
)

enum class IssuedStatus {
    PENDING, PROCESSING, ISSUED, FAILED
}

/**
 * 쿠폰 발급 사전 검증 결과
 */
data class CouponIssueValidationResult(
    val isValid: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = CouponIssueValidationResult(isValid = true)
        fun failure(errorCode: String, errorMessage: String) = 
            CouponIssueValidationResult(isValid = false, errorCode = errorCode, errorMessage = errorMessage)
            
        const val ERROR_DUPLICATE_ISSUE = "DUPLICATE_ISSUE"
        const val ERROR_OUT_OF_STOCK = "OUT_OF_STOCK"
        const val ERROR_COUPON_NOT_FOUND = "COUPON_NOT_FOUND"
    }
}