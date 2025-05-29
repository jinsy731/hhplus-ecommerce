package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload

interface OrderEventPublisher {
    fun publishPaymentCompleted(payload: PaymentCompletedPayload)
    fun publishOrderCompleted(payload: OrderCompletedPayload)
    fun publishOrderFailed(payload: OrderEventPayload)
}