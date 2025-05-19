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

    fun getIssueRequestedCouponIdListKey(): String {
        return "coupon:issue:requested-coupon-ids"
    }

    fun getFailedIssueRequestedCouponIdListKey(): String {
        return "coupon:issue:failed-requested-coupon-ids"
    }

    fun getOutOfStockCouponIdListKey(): String {
        return "coupon:issue:out-of-stock-coupon-ids"
    }
}