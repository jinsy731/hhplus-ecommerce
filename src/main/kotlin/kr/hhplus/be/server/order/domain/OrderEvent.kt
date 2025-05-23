package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.DomainEvent

sealed class OrderEvent {
    data class Created(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "order.created"
    }

    data class Prepared(override val payload: OrderSagaContext) : DomainEvent<OrderSagaContext>() {
        override val eventType: String = "order.prepared"
    }// PREPARED

    data class Completed(override val payload: OrderSagaContext) : DomainEvent<OrderSagaContext>() {
        override val eventType: String = "order.completed"
    }

    data class Failed(override val payload: OrderSagaContext) : DomainEvent<OrderSagaContext>() {
        override val eventType: String = "order.failed"
    }

    data class CouponApplyFailed(override val payload: OrderSagaContext) : DomainEvent<OrderSagaContext>() {
        override val eventType: String = "order.coupon.apply.failed"
    }
}