package kr.hhplus.be.server.coupon.application

object CouponKeyGenerator {
    fun getIssueRequestQueueKey(couponId: Long): String {
        return "coupon:$couponId:issue:request"
    }

    fun getIssuedUserSetKey(couponId: Long): String {
        return "coupon:$couponId:issued-users"
    }

    fun getStockKey(couponId: Long): String {
        return "coupon:$couponId:stock"
    }

    fun getIssuedStatusKey(userId: Long, couponId: Long): String {
        return "user:$userId:coupon:$couponId:issue-status"
    }

    fun getFailedIssueRequestQueueKey(couponId: Long): String {
        return "coupon:$couponId:issue:failed-requests"
    }
}