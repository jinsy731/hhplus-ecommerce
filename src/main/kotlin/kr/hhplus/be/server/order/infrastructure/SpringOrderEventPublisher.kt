package kr.hhplus.be.server.order.infrastructure

import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderEventPublisher
import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload
import kr.hhplus.be.server.shared.event.MessageProducer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringOrderEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
    private val messageProducer: MessageProducer
) : OrderEventPublisher {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun publishPaymentCompleted(payload: PaymentCompletedPayload) {
        val event = OrderEvent.PaymentCompleted(payload)
        eventPublisher.publishEvent(event)
        logger.info("Published PaymentCompleted event: orderId={}", payload.orderId)
    }
    
    override fun publishOrderCompleted(payload: OrderCompletedPayload) {
        val event = OrderEvent.Completed(payload)
        messageProducer.publish(event)
        eventPublisher.publishEvent(event)
        logger.info("Published OrderCompleted event: orderId={}", payload.orderId)
    }
    
    override fun publishOrderFailed(payload: OrderEventPayload) {
        val event = OrderEvent.Failed(payload)
        eventPublisher.publishEvent(event)
        logger.info("Published OrderFailed event: orderId={}", payload.orderId)
    }
} 