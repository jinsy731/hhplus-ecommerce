package kr.hhplus.be.server.shared.event

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

data class KafkaEventMessage<T: Any> (
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("occurredAt")
    val occurredAt: LocalDateTime,
    
    @JsonProperty("payload")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    val payload: T,
    
    @JsonProperty("version")
    val version: String = "v1"
) 