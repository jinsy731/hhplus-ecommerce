package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItems
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * Order 정보를 추상화한 인터페이스
 * Coupon 도메인에서 Order 도메인의 구체적인 구현에 의존하지 않기 위함
 */
data class OrderInfo(
    val id: Long,
    val userId: Long,
    val items: List<OrderItemInfo>,
    val originalTotal: Money,
    val discountedAmount: Money
) {
    /**
     * 최종 결제 금액 계산
     */
    fun finalTotal(): Money = originalTotal - discountedAmount
}

/**
 * OrderItem 정보를 추상화한 인터페이스
 */
data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val variantId: Long,
    val quantity: Int,
    val unitPrice: Money,
    val discountAmount: Money = Money.ZERO,
    val subTotal: Money = Money.ZERO,
)

fun Order.toOrderInfo(): OrderInfo = OrderInfo(
    id = this.id!!,
    userId = this.userId,
    items = this.orderItems.toOrderItemInfo(),
    originalTotal = this.originalTotal,
    discountedAmount = this.discountedAmount
)

fun OrderItems.toOrderItemInfo(): List<OrderItemInfo> = this.asList().map { OrderItemInfo(
    id = it.id ?: throw IllegalStateException("OrderItem id must not be null after save"),
    productId = it.productId,
    variantId = it.variantId,
    quantity = it.quantity,
    unitPrice = it.unitPrice,
    discountAmount = it.discountAmount,
    subTotal = it.subTotal
) }

/**
 * OrderInfo를 Payment Client 요청으로 변환하는 확장 함수들
 * Saga에서 lazy loading 문제 없이 사용 가능
 */
fun OrderInfo.toDeductUserPointRequest(timestamp: LocalDateTime): DeductUserPointRequest {
    return DeductUserPointRequest(
        orderId = this.id,
        userId = this.userId,
        amount = this.finalTotal(),
        timestamp = timestamp
    )
}

fun OrderInfo.toProcessPaymentRequest(
    pgPaymentId: String,
    paymentMethod: String,
    timestamp: LocalDateTime
): ProcessPaymentRequest {
    return ProcessPaymentRequest(
        orderId = this.id,
        userId = this.userId,
        amount = this.finalTotal(),
        pgPaymentId = pgPaymentId,
        paymentMethod = paymentMethod,
        timestamp = timestamp
    )
}

fun OrderInfo.toRestoreUserPointRequest(timestamp: LocalDateTime): RestoreUserPointRequest {
    return RestoreUserPointRequest(
        orderId = this.id,
        userId = this.userId,
        amount = this.finalTotal(),
        timestamp = timestamp
    )
}

/**
 * OrderInfo를 Coupon Client 요청으로 변환하는 확장 함수들
 */
fun OrderInfo.toCouponOrderItems(): List<CouponOrderItem> {
    return this.items.map { orderItem ->
        CouponOrderItem(
            orderItemId = orderItem.id,
            productId = orderItem.productId,
            variantId = orderItem.variantId,
            quantity = orderItem.quantity,
            price = orderItem.unitPrice.amount.toLong()
        )
    }
}

/**
 * OrderInfo를 Product Client 요청으로 변환하는 확장 함수들
 */
fun OrderInfo.toStockItems(): List<StockItem> {
    return this.items.map { orderItem ->
        StockItem(
            productId = orderItem.productId,
            variantId = orderItem.variantId,
            quantity = orderItem.quantity
        )
    }
}
