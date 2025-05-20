package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.shared.domain.DomainEvent
import java.math.BigDecimal

class OrderEvent {
    data class Completed(
        override val eventType: String = "order.completed",
        override val payload: OrderEventPayload.Order
    ): DomainEvent<OrderEventPayload.Order>()
}

class OrderEventPayload {
    data class Order(
        val orderId: Long,
        val items: List<OrderItem>,
        val totalAmount: BigDecimal,
    )
    data class OrderItem(
        val productId: Long,
        val quantity: Long,
        val subTotal: BigDecimal,
    )
}