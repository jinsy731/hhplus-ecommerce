package kr.hhplus.be.server.payment.domain

import kr.hhplus.be.server.shared.domain.DomainEvent
import java.time.LocalDateTime

data class PaymentEventPayload(
    val orderId: Long,
    val paymentId: Long?,
    val pgPaymentId: String?,
    val amount: kr.hhplus.be.server.shared.domain.Money,
    val timestamp: LocalDateTime,
    val failedReason: String? = null
)

class PaymentEvent {
    data class Initialized(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.initialized"
    }

    data class Completed(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.completed"
    }

    data class InitializingFailed(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.initializing.failed"
    }

    data class Failed(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.failed"
    }

    data class FailureFailed(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.failure.failed"
    }

    data class Canceled(
        override val payload: PaymentEventPayload): DomainEvent<PaymentEventPayload>() {
        override val eventType: String = "payment.canceled"
    }
}