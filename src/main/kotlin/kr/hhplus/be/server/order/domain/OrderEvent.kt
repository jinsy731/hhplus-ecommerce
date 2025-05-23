package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.domain.DomainEvent

class OrderEvent {
    data class Completed(
        override val payload: OrderEventPayload.Completed
    ): DomainEvent<OrderEventPayload.Completed>() {
        override val eventType: String = "order.completed"
    }
}

class OrderEventPayload {
    data class Completed(val order: Order)
}