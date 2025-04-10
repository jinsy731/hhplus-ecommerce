package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.coupon.application.CouponCommand
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderItemCommand
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.product.domain.Product
import java.time.LocalDateTime

class OrderCriteria {
    data class PlaceOrder(
        val userId: Long,
        val orderItem: List<OrderItem>,
        val userCouponIds: List<Long>,
        val now: LocalDateTime = LocalDateTime.now()
    ) {
        fun toOrderCommand(products: List<Product>) = OrderCommand.Create(userId, products, orderItem.map { it.toOrderItemCommand() }, now)
        fun toCouponCommand(order: Order) = CouponCommand.ApplyToOrder(userId, order, userCouponIds, now)
    }

    data class OrderItem(
        val productId: Long,
        val variantId: Long,
        val quantity: Int
    ) {
        fun toOrderItemCommand() = OrderItemCommand.Create(productId, variantId, quantity)
    }
}