package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal

/**
 * Order 정보를 추상화한 인터페이스
 * Coupon 도메인에서 Order 도메인의 구체적인 구현에 의존하지 않기 위함
 */
data class OrderInfo(
    val id: Long,
    val userId: Long,
    val items: List<OrderItemInfo>,
    val originalTotal: BigDecimal,
    val discountedAmount: BigDecimal
)

/**
 * OrderItem 정보를 추상화한 인터페이스
 */
data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val variantId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal
)

fun Order.toOrderInfo(): OrderInfo = OrderInfo(
    id = this.id,
    userId = this.userId,
    items = this.orderItems.toOrderItemInfo(),
    originalTotal = this.originalTotal,
    discountedAmount = this.discountedAmount
)

fun List<OrderItem>.toOrderItemInfo(): List<OrderItemInfo> = this.map { OrderItemInfo(
    id = it.id,
    productId = it.productId,
    variantId = it.variantId,
    quantity = it.quantity,
    unitPrice = it.unitPrice
) }
