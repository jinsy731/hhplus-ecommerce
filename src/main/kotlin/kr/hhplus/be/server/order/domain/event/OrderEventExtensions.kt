package kr.hhplus.be.server.order.domain.event

import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.application.OrderInfo
import kr.hhplus.be.server.order.application.OrderItemInfo
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import kr.hhplus.be.server.order.domain.model.OrderItems
import java.time.LocalDateTime

/**
 * Order 도메인 모델을 이벤트 페이로드로 변환하는 확장 함수들
 */

fun Order.toOrderEventPayload(
    userCouponIds: List<Long> = emptyList(),
    discountInfos: List<DiscountInfo> = emptyList(),
    paymentId: Long? = null,
    pgPaymentId: String? = null,
    failedReason: String? = null,
    timestamp: LocalDateTime = this.createdAt
): OrderEventPayload {
    return OrderEventPayload(
        orderId = this.id!!,
        userId = this.userId,
        originalTotal = this.originalTotal,
        discountedAmount = this.discountedAmount,
        orderItems = this.orderItems.toOrderItemEventPayload(),
        timestamp = timestamp,
        userCouponIds = userCouponIds,
        discountInfos = discountInfos,
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        failedReason = failedReason
    )
}

fun Order.toOrderSheetCreatedPayload(
    userCouponIds: List<Long>,
    timestamp: LocalDateTime = this.createdAt
): OrderSheetCreatedPayload {
    return OrderSheetCreatedPayload(
        orderId = this.id!!,
        userId = this.userId,
        originalTotal = this.originalTotal,
        orderItems = this.orderItems.toOrderItemEventPayload(),
        userCouponIds = userCouponIds,
        timestamp = timestamp
    )
}

fun Order.toPaymentRequestPayload(
    pgPaymentId: String,
    paymentMethod: String,
    timestamp: LocalDateTime = LocalDateTime.now()
): PaymentRequestPayload {
    return PaymentRequestPayload(
        orderId = this.id!!,
        userId = this.userId,
        amount = this.finalTotal(),
        pgPaymentId = pgPaymentId,
        paymentMethod = paymentMethod,
        timestamp = timestamp
    )
}

fun Order.toCouponAppliedPayload(
    userCouponIds: List<Long>,
    discountInfos: List<DiscountInfo>,
    timestamp: LocalDateTime = LocalDateTime.now()
): CouponAppliedPayload {
    return CouponAppliedPayload(
        orderId = this.id!!,
        userId = this.userId,
        userCouponIds = userCouponIds,
        discountInfos = discountInfos,
        timestamp = timestamp
    )
}

fun Order.toStockDeductedPayload(
    timestamp: LocalDateTime = LocalDateTime.now()
): StockDeductedPayload {
    return StockDeductedPayload(
        orderId = this.id!!,
        userId = this.userId,
        orderItems = this.orderItems.toOrderItemEventPayload(),
        timestamp = timestamp
    )
}

fun Order.toOrderCompletedPayload(
    paymentId: Long,
    pgPaymentId: String,
    timestamp: LocalDateTime = LocalDateTime.now()
): OrderCompletedPayload {
    return OrderCompletedPayload(
        orderId = this.id!!,
        userId = this.userId,
        originalTotal = this.originalTotal,
        finalTotal = this.finalTotal(),
        orderItems = this.orderItems.toOrderItemEventPayload(),
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        timestamp = timestamp
    )
}

/**
 * OrderInfo를 사용하는 확장 함수들 - lazy loading 문제 해결
 */

fun OrderInfo.toOrderEventPayload(
    userCouponIds: List<Long> = emptyList(),
    discountInfos: List<DiscountInfo> = emptyList(),
    paymentId: Long? = null,
    pgPaymentId: String? = null,
    failedReason: String? = null,
    timestamp: LocalDateTime
): OrderEventPayload {
    return OrderEventPayload(
        orderId = this.id,
        userId = this.userId,
        originalTotal = this.originalTotal,
        discountedAmount = this.discountedAmount,
        orderItems = this.items.toOrderItemEventPayload(),
        timestamp = timestamp,
        userCouponIds = userCouponIds,
        discountInfos = discountInfos,
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        failedReason = failedReason
    )
}

fun OrderInfo.toOrderCompletedPayload(
    paymentId: Long,
    pgPaymentId: String,
    timestamp: LocalDateTime
): OrderCompletedPayload {
    return OrderCompletedPayload(
        orderId = this.id,
        userId = this.userId,
        originalTotal = this.originalTotal,
        finalTotal = this.finalTotal(),
        orderItems = this.items.toOrderItemEventPayload(),
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        timestamp = timestamp
    )
}

fun OrderInfo.toStockDeductedPayload(
    timestamp: LocalDateTime
): StockDeductedPayload {
    return StockDeductedPayload(
        orderId = this.id,
        userId = this.userId,
        orderItems = this.items.toOrderItemEventPayload(),
        timestamp = timestamp
    )
}

fun OrderInfo.toCouponAppliedPayload(
    userCouponIds: List<Long>,
    discountInfos: List<DiscountInfo>,
    timestamp: LocalDateTime
): CouponAppliedPayload {
    return CouponAppliedPayload(
        orderId = this.id,
        userId = this.userId,
        userCouponIds = userCouponIds,
        discountInfos = discountInfos,
        timestamp = timestamp
    )
}

fun OrderItems.toOrderItemEventPayload(): List<OrderItemEventPayload> {
    return this.asList().map { it.toOrderItemEventPayload() }
}

fun OrderItem.toOrderItemEventPayload(): OrderItemEventPayload {
    return OrderItemEventPayload(
        id = this.id!!,
        productId = this.productId,
        variantId = this.variantId,
        quantity = this.quantity,
        unitPrice = this.unitPrice,
        discountAmount = this.discountAmount
    )
}

fun List<OrderItemInfo>.toOrderItemEventPayload(): List<OrderItemEventPayload> {
    return this.map { it.toOrderItemEventPayload() }
}

fun OrderItemInfo.toOrderItemEventPayload(): OrderItemEventPayload {
    return OrderItemEventPayload(
        id = this.id,
        productId = this.productId,
        variantId = this.variantId,
        quantity = this.quantity,
        unitPrice = this.unitPrice,
        discountAmount = this.discountAmount
    )
} 