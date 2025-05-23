package kr.hhplus.be.server.order.entrypoint.event

import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderResultSender
import kr.hhplus.be.server.payment.domain.PaymentEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringOrderEventListener(
    private val orderService: OrderService,
    private val orderResultSender: OrderResultSender
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(OrderEvent.Completed::class)
    fun onOrderCompleted(event: OrderEvent.Completed) {
        logger.info("[{}] Received OrderEvent.Completed: orderId={}", 
            Thread.currentThread().name, event.payload.order.id)
        orderResultSender.send(event.payload.order)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Completed) {
        logger.info("[{}] Received PaymentEvent.Completed: paymentId={}, orderId={}", 
            Thread.currentThread().name, event.payload.paymentId, event.payload.order.id)
        orderService.completeOrder(event.payload.order.id, event.payload)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: CouponEvent.Used) {
        logger.info("[{}] Received CouponEvent.Used: orderId={}, userCouponIds={}", 
            Thread.currentThread().name, event.payload.order.id, event.payload.userCouponIds)
        orderService.applyDiscount(
            OrderCommand.ApplyDiscount(
            orderId = event.payload.order.id,
            discountInfos = event.payload.discountInfos,
            context = event.payload))
    }
}