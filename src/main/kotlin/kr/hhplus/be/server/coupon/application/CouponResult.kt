package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import java.time.LocalDateTime

class CouponResult {
    data class ApplyToOrder(
        val discountLine: List<DiscountLine>
    )

    data class Issue(
        val userCouponId: Long?,
        val status: UserCouponStatus,
        val expiredAt: LocalDateTime
    )
}