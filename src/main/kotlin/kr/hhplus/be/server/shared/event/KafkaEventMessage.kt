package kr.hhplus.be.server.shared.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class KafkaEventMessage<T: Any> (
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("occurredAt")
    val occurredAt: LocalDateTime,
    
    @JsonProperty("payload")
    val payload: T,
    
    @JsonProperty("version")
    val version: String = "v1"
) 