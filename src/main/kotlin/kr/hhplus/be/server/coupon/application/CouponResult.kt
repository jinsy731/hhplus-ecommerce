package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import java.math.BigDecimal

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
}