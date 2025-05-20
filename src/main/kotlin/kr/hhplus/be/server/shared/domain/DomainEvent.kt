package kr.hhplus.be.server.shared.domain

import java.time.LocalDateTime
import java.util.UUID

abstract class DomainEvent<T: Any>(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
) {
    abstract val eventType: String
    abstract val payload: T
}

