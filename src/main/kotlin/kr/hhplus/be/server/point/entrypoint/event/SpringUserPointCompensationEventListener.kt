package kr.hhplus.be.server.point.entrypoint.event

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.point.application.UserPointCommand
import kr.hhplus.be.server.point.application.UserPointService
import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringUserPointCompensationEventListener(
    private val userPointService: UserPointService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Failed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.Failed) = compensate(event)

    private fun compensate(event: DomainEvent<OrderSagaContext>) {
        logger.info("[{}] Received {}: userId={}, amount={}, reason={}",
            Thread.currentThread().name, event.eventType, event.payload.order.userId, event.payload.order.finalTotal(), event.payload.failedReason)
        userPointService.restore(
            UserPointCommand.Restore(
            userId = event.payload.order.userId,
            amount = event.payload.order.finalTotal(),
            now = event.payload.timestamp,
            context = event.payload
        ))
    }
} 