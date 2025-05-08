package kr.hhplus.be.server.order.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.order.domain.OrderItemStatus
import org.junit.jupiter.api.Test

class OrderItemTest {
    
    @Test
    fun `✅주문 항목 소계 계산_quantity와 unitPrice의 곱이어야 한다`() {
        // arrange
        val orderItem = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 10,
            unitPrice = Money.of(1000),
            status = OrderItemStatus.ORDERED
        )
        // act, assert
        orderItem.subTotal shouldBe Money.of(10000)
    }
    
    @Test
    fun `✅할인 적용_할인가격 만큼 discountAmount가 증가해야 한다`() {
        // arrange
        val orderItem = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 10,
            unitPrice = Money.of(1000),
            discountAmount = Money.ZERO,
            status = OrderItemStatus.ORDERED
        )
        // act
        orderItem.applyDiscount(Money.of(100))
        
        // assert
        orderItem.discountAmount shouldBe Money.of(100)
    }
}