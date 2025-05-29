package kr.hhplus.be.server.product.domain.product

import kr.hhplus.be.server.shared.domain.DomainEvent
import java.time.LocalDateTime

data class ProductEventPayload(
    val orderId: Long,
    val items: List<ProductStockItem>,
    val timestamp: LocalDateTime,
    val failedReason: String? = null
)

data class ProductStockItem(
    val productId: Long,
    val variantId: Long,
    val quantity: Int
)

/**
 * Product 도메인 이벤트들
 * 
 * @deprecated 새로운 동기적 주문 프로세스에서는 더 이상 사용되지 않습니다.
 * 기존 비동기 이벤트 핸들러들과의 호환성을 위해서만 유지됩니다.
 */
class ProductEvent {
    @Deprecated("Direct synchronous processing is used instead")
    data class StockDeducted(
        override val payload: ProductEventPayload): DomainEvent<ProductEventPayload>() {
        override val eventType: String = "product.stock-deducted"
    }

    @Deprecated("Direct synchronous processing is used instead")
    data class StockDeductionFailed(
        override val payload: ProductEventPayload): DomainEvent<ProductEventPayload>() {
        override val eventType: String = "product.stock-deduction-failed"
    }
}