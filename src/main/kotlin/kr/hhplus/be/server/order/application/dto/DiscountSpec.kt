package kr.hhplus.be.server.order.application.dto

import java.math.BigDecimal

class DiscountSpec {
    class Coupon {
        data class Root(
            val userId: Long,
            val userCouponIds: List<Long>,
            val items: List<Item>
        )
        data class Item(
            val productId: Long,
            val variantId: Long,
            val quantity: Int,
            val subTotal: BigDecimal
        )
    }
}