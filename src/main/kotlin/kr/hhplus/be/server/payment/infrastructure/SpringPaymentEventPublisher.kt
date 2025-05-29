package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.payment.domain.PaymentEventPayload
import kr.hhplus.be.server.payment.domain.PaymentEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringPaymentEventPublisher(
    private val eventPublisher: ApplicationEventPublisher
) : PaymentEventPublisher {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun publishPaymentFailed(payload: PaymentEventPayload) {
        val event = PaymentEvent.Failed(payload)
        eventPublisher.publishEvent(event)
        logger.info("Published PaymentFailed event: orderId={}, paymentId={}, reason={}", 
            payload.orderId, payload.paymentId, payload.failedReason)
    }
    
    override fun publishPaymentFailureFailed(payload: PaymentEventPayload) {
        val event = PaymentEvent.FailureFailed(payload)
        eventPublisher.publishEvent(event)
        logger.info("Published PaymentFailureFailed event: orderId={}, paymentId={}", 
            payload.orderId, payload.paymentId)
    }
} 