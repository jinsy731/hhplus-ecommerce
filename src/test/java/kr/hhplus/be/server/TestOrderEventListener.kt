package kr.hhplus.be.server

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload
import kr.hhplus.be.server.shared.event.KafkaEventMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class TestOrderEventListener(
    private val objectMapper: ObjectMapper
) {
    companion object {
        var lastReceivedPayload: OrderCompletedPayload? = null

        fun reset() {
            lastReceivedPayload = null
        }
    }

    @KafkaListener(topics = ["order_completed.v1"], groupId = "test-order-service-group")
    fun onOrderCompleted(
        @Payload message: KafkaEventMessage<OrderCompletedPayload>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        println("Received OrderCompleted event: topic=$topic, partition=$partition, offset=$offset, eventId=${message.eventId}")
        
        lastReceivedPayload = message.payload
        acknowledgment.acknowledge()
    }
}