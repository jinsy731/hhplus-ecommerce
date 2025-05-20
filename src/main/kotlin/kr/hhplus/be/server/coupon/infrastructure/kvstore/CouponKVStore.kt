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