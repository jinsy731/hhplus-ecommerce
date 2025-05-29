package kr.hhplus.be.server.rank.entrypoint.event

import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload
import kr.hhplus.be.server.rank.application.RankingCommand
import kr.hhplus.be.server.rank.application.RankingService
import kr.hhplus.be.server.shared.event.KafkaEventMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class KafkaRankingEventListener(
    private val rankingService: RankingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["order_completed.v1"], groupId = "ranking-service-group")
    fun onOrderCompleted(
        @Payload message: KafkaEventMessage<OrderCompletedPayload>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received OrderCompleted event from Kafka: topic={}, partition={}, offset={}, eventId={}",
                topic, partition, offset, message.eventId)

            // payload를 OrderCompletedPayload로 변환
            val payload = message.payload
            
            val rankingItems = payload.orderItems.map { orderItem ->
                RankingCommand.UpdateProductRanking.Item(
                    productId = orderItem.productId,
                    quantity = orderItem.quantity.toLong()
                )
            }
            
            rankingService.updateProductRanking(
                RankingCommand.UpdateProductRanking.Root(
                    items = rankingItems,
                    timestamp = payload.timestamp
                )
            )
            
            acknowledgment.acknowledge()
            logger.info("Successfully processed OrderCompleted event: eventId={}", message.eventId)
        } catch (e: Exception) {
            logger.error("Failed to process OrderCompleted event: eventId={}, error={}", message.eventId, e.message, e)
        }
    }
} 