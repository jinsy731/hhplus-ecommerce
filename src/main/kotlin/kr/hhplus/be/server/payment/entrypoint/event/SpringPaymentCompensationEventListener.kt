package kr.hhplus.be.server.payment.entrypoint.event

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.point.domain.UserPointEvent
import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringPaymentCompensationEventListener(
    private val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserPointEvent.DeductionFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.Failed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Failed) = compensate(event)

    private fun compensate(event: DomainEvent<OrderSagaContext>) {
        logger.info("[{}] Received {}}: paymentId={}, reason={}",
            Thread.currentThread().name, event.eventType, event.payload.paymentId, event.payload.failedReason)
        paymentService.cancelPayment(
            PaymentCommand.Cancel(
            paymentId = event.payload.paymentId!!,
            context = event.payload
        ))
    }
} 