package kr.hhplus.be.server.order

import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import kr.hhplus.be.server.order.domain.model.OrderItemStatus
import kr.hhplus.be.server.order.domain.model.OrderStatus
import java.math.BigDecimal

object OrderTestFixture {
    fun createOrder(userId: Long) = Order(
        id = 1L,
        userId = userId,
        status = OrderStatus.CREATED,
        originalTotal = BigDecimal(10000),
        discountedAmount = BigDecimal(9000),
        orderItems = createOrderItems()
    )

    fun createOrderItems() = mutableListOf(
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