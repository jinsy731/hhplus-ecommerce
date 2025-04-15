package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponCommand {
    class Use {
        data class Root(
            val userId: Long,
            val userCouponIds: List<Long>,
            val totalAmount: BigDecimal,
            val items: List<Item>,
            val timestamp: LocalDateTime
        )
        data class Item(
            val orderItemId: Long,
            val productId: Long,
            val variantId: Long,
            val quantity: Int,
            val subTotal: BigDecimal
        )
    }

    data class Issue(
        val userId: Long,
        val couponId: Long
    )
}

fun CouponCommand.Use.Root.toDiscountContext(): DiscountContext.Root = DiscountContext.Root(
    userId = this.userId,
    totalAmount = this.totalAmount,
    items = this.items.toDiscountContext(this.totalAmount),
    timestamp = this.timestamp,
)

fun List<CouponCommand.Use.Item>.toDiscountContext(totalAmount: BigDecimal): List<DiscountContext.Item> = this.map { item -> DiscountContext.Item(
    orderItemId = item.orderItemId,
    productId = item.productId,
    variantId = item.variantId,
    quantity = item.quantity,
    subTotal = item.subTotal,
    totalAmount = totalAmount
) }