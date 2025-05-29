package kr.hhplus.be.server.order.domain.client

import kr.hhplus.be.server.order.domain.model.OrderItem

/**
 * Order 도메인에서 Product 도메인을 호출하기 위한 클라이언트 인터페이스
 */
interface ProductClient {
    
    /**
     * 재고 검증 및 차감
     */
    fun validateAndReduceStock(request: ReduceStockRequest): Result<ReduceStockResponse>
    
    /**
     * 재고 복구 (결제 실패 시)
     */
    fun restoreStock(request: RestoreStockRequest): Result<Unit>
}

/**
 * 재고 차감 요청
 */
data class ReduceStockRequest(
    val orderId: Long,
    val items: List<StockItem>
)

/**
 * 재고 아이템
 */
data class StockItem(
    val productId: Long,
    val variantId: Long,
    val quantity: Int
)

/**
 * 재고 차감 응답
 */
data class ReduceStockResponse(
    val orderId: Long,
    val processedItems: List<ProcessedStockItem>
)

/**
 * 처리된 재고 아이템
 */
data class ProcessedStockItem(
    val productId: Long,
    val requestedQuantity: Int,
    val processedQuantity: Int
)

/**
 * 재고 복구 요청
 */
data class RestoreStockRequest(
    val orderId: Long,
    val items: List<StockItem>
)

/**
 * Order 도메인 모델을 클라이언트 요청으로 변환하는 확장 함수들
 */
fun List<OrderItem>.toStockItems(): List<StockItem> {
    return this.map { orderItem ->
        StockItem(
            productId = orderItem.productId,
            variantId = orderItem.variantId,
            quantity = orderItem.quantity
        )
    }
} 