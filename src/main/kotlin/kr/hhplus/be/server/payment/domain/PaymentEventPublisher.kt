package kr.hhplus.be.server.payment.domain

interface PaymentEventPublisher {
    fun publishPaymentFailed(payload: PaymentEventPayload)
    fun publishPaymentFailureFailed(payload: PaymentEventPayload)
}