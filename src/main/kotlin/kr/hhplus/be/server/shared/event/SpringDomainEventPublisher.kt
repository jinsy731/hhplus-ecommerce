package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(
    private val publisher: ApplicationEventPublisher,
    private val messageProducer: MessageProducer
): DomainEventPublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun <T : Any> publish(event: DomainEvent<T>) {
        // 기존 Spring 이벤트 발행 (로컬 처리용)
        publisher.publishEvent(event)
        
        // Kafka로 이벤트 발행 (분산 처리용)
        messageProducer.publish(event)
        
        logger.info("[{}] Event Published: {} {}", Thread.currentThread().name, event.eventId, event.eventType)
    }
}