package kr.hhplus.be.server.coupon.domain

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.DomainEvent

class CouponEvent {
    data class Used(override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "coupon.used"
    }

    data class UseFailed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "coupon.use-failed"
    }

    data class UseRestored(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "coupon.use-restored"
    }
}