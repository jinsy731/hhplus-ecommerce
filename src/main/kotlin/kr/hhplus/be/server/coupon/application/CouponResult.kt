package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponResult {
    data class ApplyToOrder(
        val discountLine: List<DiscountLine>
    )

    data class AppliedDiscount(
        val sourceId: Long,
        val discountSubTotal: BigDecimal,
        val discountMethod: DiscountMethod,
        val discountPerItem: List<DiscountPerItem>,
    )

    data class DiscountPerItem(
        val orderItemId: Long,
        val discountAmount: BigDecimal
    )

    data class Issue(
        val userCouponId: Long?,
        val status: UserCouponStatus,
        val expiredAt: LocalDateTime
    )
}