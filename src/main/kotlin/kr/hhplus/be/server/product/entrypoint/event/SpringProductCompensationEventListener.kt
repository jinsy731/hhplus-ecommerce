package kr.hhplus.be.server.product.entrypoint.event

import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.point.domain.UserPointEvent
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.product.application.dto.ProductCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringProductCompensationEventListener(
    private val productService: ProductService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.InitializingFailed) {
        logger.info("[{}] Received PaymentEvent.InitializingFailed: orderId={}, items={}, reason={}", 
            Thread.currentThread().name, event.payload.order.id, event.payload.order.orderItems, event.payload.failedReason)
        productService.restoreStock(ProductCommand.RestoreStock.Root(
            items = event.payload.order.orderItems.map {
            ProductCommand.RestoreStock.Item(
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity,
            )
        }, context = event.payload))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentEvent.Failed) {
        logger.info("[{}] Received PaymentEvent.Failed: orderId={}, items={}, reason={}", 
            Thread.currentThread().name, event.payload.order.id, event.payload.order.orderItems, event.payload.failedReason)
        productService.restoreStock(ProductCommand.RestoreStock.Root(
            items = event.payload.order.orderItems.map {
                ProductCommand.RestoreStock.Item(
                    productId = it.productId,
                    variantId = it.variantId,
                    quantity = it.quantity,
                )
            }, context = event.payload))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserPointEvent.DeductionFailed) {
        logger.info("[{}] Received UserPointEvent.DeductionFailed: orderId={}, items={}, reason={}", 
            Thread.currentThread().name, event.payload.order.id, event.payload.order.orderItems, event.payload.failedReason)
        productService.restoreStock(ProductCommand.RestoreStock.Root(
            items = event.payload.order.orderItems.map {
                ProductCommand.RestoreStock.Item(
                    productId = it.productId,
                    variantId = it.variantId,
                    quantity = it.quantity,
                )
            }, context = event.payload))
    }
} 