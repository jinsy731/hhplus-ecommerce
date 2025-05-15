package kr.hhplus.be.server.coupon.infrastructure

interface CouponKVStore {
    fun existsIssuedUser(userId: Long, couponId: Long)
    fun setIssuedUser(userId: Long, couponId: Long)

    fun pushToIssueReqeustQueue(issueRequest: CouponIssueRequest)
    fun popBatchFromIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>

    fun pushToFailedIssueRequestQueue(issueRequest: CouponIssueRequest)
    fun pushAllToFailedIssueRequestQueue(failedRequests: List<CouponIssueRequest>)
    fun popBatchFromFailedIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest>

    fun getStock(couponId: Long): CouponStock
    fun getStocks(couponIds: List<Long>): CouponStock
    fun setStock(couponStock: CouponStock)

    fun setIssuedStatus(userId: Long, couponId: Long, status: IssuedStatus)
    fun getIssuedStatus(userId: Long, couponId: Long): IssuedStatus

    fun addToIssueRequestedCouponIdSet(couponId: Long)
    fun popFromIssueRequestedCouponIdSet(): Long?
    fun addToFailedIssueRequestedCouponIdSet(couponId: Long)
    fun popFromFailedIssueRequestedCouponIdSet(): Long?
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
    PROCESSING, ISSUED, FAILED
}