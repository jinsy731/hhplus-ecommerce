package kr.hhplus.be.server.coupon.entrypoint.event

import kr.hhplus.be.server.coupon.application.CouponIssueConsumerService
import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.coupon.domain.CouponIssueRequestedPayload
import kr.hhplus.be.server.shared.event.KafkaEventMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class KafkaCouponEventListener(
    private val couponIssueConsumerService: CouponIssueConsumerService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["coupon_issue-requested.v1"],
        groupId = "coupon-issue-consumer-group",
        batch = "true"
    )
    fun onCouponIssueRequested(
        @Payload messages: List<KafkaEventMessage<CouponIssueRequestedPayload>>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: IntArray,
        @Header(KafkaHeaders.OFFSET) offset: LongArray,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("쿠폰 발급 요청 배치 수신: topic={}, 배치크기={}", topic, messages.size)

            val events = messages.map { message ->
                CouponEvent.IssueRequested(payload = message.payload)
            }

            couponIssueConsumerService.processCouponIssueRequestsBatch(events)

            acknowledgment.acknowledge()
            logger.info("쿠폰 발급 배치 처리 완료: 처리된 메시지 수={}", events.size)

        } catch (e: Exception) {
            logger.error("쿠폰 발급 배치 처리 실패: 배치크기={}, error={}", messages.size, e.message, e)
            // TODO: DLQ 처리 또는 재시도 로직 추가
        }
    }
}