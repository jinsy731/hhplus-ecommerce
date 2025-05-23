package kr.hhplus.be.server.point.domain

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.DomainEvent

class UserPointEvent {
    data class Deducted(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "userPoint.deducted"
    }

    data class DeductionFailed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "userPoint.deduction-failed"
    }
}