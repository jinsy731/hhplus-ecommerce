package kr.hhplus.be.server.order

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.order.domain.OrderItemStatus
import kr.hhplus.be.server.order.domain.OrderStatus

object OrderTestFixture {
    /**
     * 1. 총액 10000원
     * 2. 할인금액 9000원
     * 3. 총 2개의 상품 주문
     * 4. 각 상품은 1000 * 5 = 5000원 - 500원 할인
     */
    fun createDiscountedOrder(userId: Long = 1L): Order {
        val order = Order(
            id = 1L,
            userId = userId,
            status = OrderStatus.CREATED,
            originalTotal = Money.of(10000),
            discountedAmount = Money.of(9000),
        )
        createDiscountedOrderItems(order).forEach { order.addItem(it) }
        return order
    }

    fun createOrder(userId: Long = 1L): Order {
        val order = Order(
            id = 1L,
            userId = userId,
            status = OrderStatus.CREATED,
            originalTotal = Money.of(10000),
            discountedAmount = Money.ZERO,
        )
        createOrderItems(order).forEach { order.addItem(it) }
        return order
    }

    fun createOrderItems(order: Order) = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.ZERO,
            status = OrderItemStatus.ORDERED,
            order = order
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.ZERO,
            status = OrderItemStatus.ORDERED,
            order = order
        ),
    )

    fun createDiscountedOrderItems(order: Order) = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.of(500),
            status = OrderItemStatus.ORDERED,
            order = order
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.of(500),
            status = OrderItemStatus.ORDERED,
            order = order
        ),
    )
}