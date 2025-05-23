package kr.hhplus.be.server.product.entrypoint.event

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.product.application.dto.ProductCommand
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringProductOrderEventListener(
    private val productService: ProductService,
    private val entityManager: EntityManager
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.Prepared) {
        productService.validateAndReduceStock(ProductCommand.ValidateAndReduceStock.Root(
            items = event.payload.order.orderItems.map { ProductCommand.ValidateAndReduceStock.Item(
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity
            ) },
            context = event.payload
        ))
    }
}