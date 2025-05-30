package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : MessageProducer {
    
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun <T : Any> publish(event: DomainEvent<T>) {
        val message = KafkaEventMessage(
            eventId = event.eventId,
            eventType = event.eventType,
            occurredAt = event.occurredAt,
            payload = event.payload
        )
        val topicName = generateTopicName(message)

        logger.info("Publishing event to Kafka: topic={}, eventId={}, eventType={}", 
            topicName, event.eventId, event.eventType)

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(topicName, event.eventId, message)
        
        future.whenComplete { result, exception ->
            if (exception == null) {
                logger.info("Successfully published event: topic={}, eventId={}, offset={}", 
                    topicName, event.eventId, result.recordMetadata.offset())
            } else {
                logger.error("Failed to publish event: topic={}, eventId={}, error={}", 
                    topicName, event.eventId, exception.message, exception)
            }
        }
    }

    private fun generateTopicName(message: KafkaEventMessage<*>): String {
        // eventType 형식: "order.created", "payment.completed" 등
        // 토픽 형식: "order_created.v1", "payment_completed.v1" 등
        return message.eventType.replace(".", "_") + "." + message.version
    }
}