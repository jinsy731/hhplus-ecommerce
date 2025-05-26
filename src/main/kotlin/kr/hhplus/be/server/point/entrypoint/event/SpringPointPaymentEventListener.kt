package kr.hhplus.be.server.point.entrypoint.event

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.point.application.UserPointCommand
import kr.hhplus.be.server.point.application.UserPointService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringPointPaymentEventListener(private val userPointService: UserPointService, private val entityManager: EntityManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Initialized) {
        logger.info("[{}] Received PaymentEvent.Initialized: paymentId={}, orderId={}", 
            Thread.currentThread().name, event.payload.paymentId, event.payload.order.id)
        userPointService.use(
            UserPointCommand.Use(
            userId = event.payload.order.userId,
            amount = event.payload.order.finalTotal(),
            now = event.payload.timestamp,
            context = event.payload))
    }
}