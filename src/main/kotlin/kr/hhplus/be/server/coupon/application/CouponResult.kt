package kr.hhplus.be.server.coupon.application

import java.math.BigDecimal

class CouponResult {
    data class ApplyToOrder(
        val totalDiscount: BigDecimal,
        val discountPerItem: List<CouponDiscountPerItem>
    )

    data class CouponDiscountPerItem(
        val orderItemId: Long,
        val discountAmount: BigDecimal
    )
}