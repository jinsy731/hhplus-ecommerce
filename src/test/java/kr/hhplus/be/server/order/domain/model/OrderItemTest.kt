package kr.hhplus.be.server.order.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderItemTest {
    
    @Test
    fun `✅주문 항목 소계 계산_quantity와 unitPrice의 곱이어야 한다`() {
        // arrange
        val orderItem = OrderItem(
            id = 1L,
            orderId = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 10,
            unitPrice = BigDecimal(1000),
            status = OrderItemStatus.ORDERED
        )
        // act, assert
        orderItem.subTotal() shouldBe BigDecimal(10000)
    }
}