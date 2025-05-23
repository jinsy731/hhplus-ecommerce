package kr.hhplus.be.server.payment.domain

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.DomainEvent

class PaymentEvent {
    data class Initialized(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "payment.initialized"
    }

    data class Completed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "payment.completed"
    }

    data class InitializingFailed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "payment.initializing.failed"
    }

    data class Failed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "payment.failed"
    }

    data class Canceled(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "payment.canceled"
    }
}