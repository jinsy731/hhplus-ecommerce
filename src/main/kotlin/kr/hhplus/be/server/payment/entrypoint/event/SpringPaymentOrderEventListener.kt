package kr.hhplus.be.server.payment.entrypoint.event

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.point.domain.UserPointEvent
import kr.hhplus.be.server.product.domain.product.ProductEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringPaymentOrderEventListener(private val paymentService: PaymentService, private val entityManager: EntityManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProductEvent.StockDeducted) {
        val items = event.payload.order.orderItems.map {
            PaymentCommand.Prepare.OrderItemInfo(
                id = it.id,
                productId = it.productId,
                variantId = it.variantId,
                subTotal = it.subTotal,
                discountedAmount = it.discountAmount,
            )
        }
        val order = PaymentCommand.Prepare.OrderInfo(
            id = event.payload.order.id,
            userId = event.payload.order.userId,
            items = items,
            originalTotal = event.payload.order.originalTotal,
            discountedAmount = event.payload.order.discountedAmount
        )
        paymentService.preparePayment(
            PaymentCommand.Prepare.Root(
            order = order,
            timestamp = event.payload.timestamp,
            context = event.payload
        ))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserPointEvent.Deducted) {
        try { paymentService.completePayment(
            PaymentCommand.Complete(
            paymentId = event.payload.paymentId!!,
            context = event.payload)) }
        catch (e: Exception) {
            logger.error("Payment failed", e)
        }
    }
}