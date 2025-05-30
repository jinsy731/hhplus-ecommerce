package kr.hhplus.be.server.coupon.entrypoint.event

import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.shared.event.MessageProducer
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ApplicationCouponEventListener(
    private val messageProducer: MessageProducer
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handleCouponIssueRequested(event: CouponEvent.IssueRequested) {
        try {
            logger.info("[handleCouponIssueRequested] 쿠폰 발급 요청 이벤트 수신: userId=${event.payload.userId}, couponId=${event.payload.couponId}")

            // MessageProducer를 통해 Kafka로 메시지 발행
            messageProducer.publish(event)

            logger.info("[handleCouponIssueRequested] Kafka 메시지 발행 성공: ${event.eventType}")

        } catch (e: Exception) {
            logger.error("[handleCouponIssueRequested] Kafka 메시지 발행 실패", e)
            // TODO: 실패 시 재시도 로직이나 DLQ 처리 등 필요한 보상 로직 추가
            throw e
        }
    }
}