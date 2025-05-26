package kr.hhplus.be.server.product.domain.product

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.DomainEvent

class ProductEvent {
    data class StockDeducted(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "product.stock-deducted"
    }

    data class StockDeductionFailed(
        override val payload: OrderSagaContext): DomainEvent<OrderSagaContext>() {
        override val eventType: String = "product.stock-deduction-failed"
    }
}