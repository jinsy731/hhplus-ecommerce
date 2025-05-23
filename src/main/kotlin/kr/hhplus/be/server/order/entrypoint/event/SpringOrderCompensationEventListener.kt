package kr.hhplus.be.server.order.entrypoint.event

import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.point.domain.UserPointEvent
import kr.hhplus.be.server.product.domain.product.ProductEvent
import kr.hhplus.be.server.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringOrderCompensationEventListener(
    private val orderService: OrderService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.CouponApplyFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: CouponEvent.UseFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProductEvent.StockDeductionFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.InitializingFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserPointEvent.DeductionFailed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Failed) = compensate(event)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.Failed) = compensate(event)

    private fun compensate(event: DomainEvent<OrderSagaContext>) {
        logger.info("[{}] Received {}: orderId={}, reason={}",
            Thread.currentThread().name, event.eventType, event.payload.order.id, event.payload.failedReason)
        orderService.fail(event.payload.order.id)
    }
} 