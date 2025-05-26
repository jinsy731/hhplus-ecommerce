package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(private val publisher: ApplicationEventPublisher): DomainEventPublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun <T : Any> publish(event: DomainEvent<T>) {
        publisher.publishEvent(event)
        logger.info("[{}] Event Published: {} {}", Thread.currentThread().name, event.eventId, event.eventType)
    }
}