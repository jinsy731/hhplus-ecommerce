package kr.hhplus.be.server.order.entrypoint.event

import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.shared.event.MessageProducer
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ApplicationOrderEventListener(
    private val messageProducer: MessageProducer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 주문 완료 이벤트 처리
     * -> 외부 시스템에 주문 결과 전송
     */
    @Async
    @TransactionalEventListener(OrderEvent.Completed::class)
    fun onOrderCompleted(event: OrderEvent.Completed) {
        logger.info("[{}] Received OrderEvent.Completed: orderId={}", 
            Thread.currentThread().name, event.payload.orderId)
        messageProducer.publish(event)
    }
}