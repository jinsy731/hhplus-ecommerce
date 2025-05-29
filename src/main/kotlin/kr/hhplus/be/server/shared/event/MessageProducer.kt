package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.shared.domain.DomainEvent

interface MessageProducer {
    fun <T: Any> publish(event: DomainEvent<T>)
} 