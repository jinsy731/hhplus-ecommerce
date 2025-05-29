package kr.hhplus.be.server.shared.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class KafkaEventMessage(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("occurredAt")
    val occurredAt: LocalDateTime,
    
    @JsonProperty("payload")
    val payload: Any,
    
    @JsonProperty("version")
    val version: String = "v1"
) 