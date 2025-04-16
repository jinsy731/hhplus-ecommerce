package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import java.time.LocalDateTime

class CouponResult {
    data class Use(
        val discountInfo: List<DiscountInfo>
    )

    data class Issue(
        val userCouponId: Long?,
        val status: UserCouponStatus,
        val expiredAt: LocalDateTime
    )
}