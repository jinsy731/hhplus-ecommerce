package kr.hhplus.be.server.coupon.application.dto

import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

class CouponCommand {
    class Use {
        data class Root(
            val orderId: Long,
            val userId: Long,
            val userCouponIds: List<Long>,
            val totalAmount: Money,
            val items: List<Item>,
            val timestamp: LocalDateTime,
        )
        data class Item(
            val orderItemId: Long,
            val productId: Long,
            val variantId: Long,
            val quantity: Int,
            val subTotal: Money
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

fun List<CouponCommand.Use.Item>.toDiscountContext(totalAmount: Money): List<DiscountContext.Item> = this.map { item -> DiscountContext.Item(
    orderItemId = item.orderItemId,
    productId = item.productId,
    variantId = item.variantId,
    quantity = item.quantity,
    subTotal = item.subTotal,
    totalAmount = totalAmount
) }