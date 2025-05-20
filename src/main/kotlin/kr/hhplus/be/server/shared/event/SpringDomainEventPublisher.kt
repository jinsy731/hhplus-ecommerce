package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.shared.domain.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(private val publisher: ApplicationEventPublisher): DomainEventPublisher {
    override fun <T : Any> publish(event: DomainEvent<T>) {
        publisher.publishEvent(event)
    }
}