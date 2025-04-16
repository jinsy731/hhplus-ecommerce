package kr.hhplus.be.server.order

import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.order.domain.OrderItemStatus
import kr.hhplus.be.server.order.domain.OrderStatus
import java.math.BigDecimal

object OrderTestFixture {
    /**
     * 1. 총액 10000원
     * 2. 할인금액 9000원
     * 3. 총 2개의 상품 주문
     * 4. 각 상품은 1000 * 5 = 5000원 - 500원 할인
     */
    fun createDiscountedOrder(userId: Long = 1L) = Order(
        id = 1L,
        userId = userId,
        status = OrderStatus.CREATED,
        originalTotal = BigDecimal(10000),
        discountedAmount = BigDecimal(9000),
        orderItems = createDiscountedOrderItems()
    )

    fun createOrder(userId: Long = 1L) = Order(
        id = 1L,
        userId = userId,
        status = OrderStatus.CREATED,
        originalTotal = BigDecimal(10000),
        discountedAmount = BigDecimal.ZERO,
        orderItems = createOrderItems()
    )

    fun createOrderItems() = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = BigDecimal(1000),
            discountAmount = BigDecimal.ZERO,
            status = OrderItemStatus.ORDERED,
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = BigDecimal(1000),
            discountAmount = BigDecimal.ZERO,
            status = OrderItemStatus.ORDERED,
        ),
    )

    fun createDiscountedOrderItems() = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = BigDecimal(1000),
            discountAmount = BigDecimal(500),
            status = OrderItemStatus.ORDERED,
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = BigDecimal(1000),
            discountAmount = BigDecimal(500),
            status = OrderItemStatus.ORDERED,
        ),
    )
}