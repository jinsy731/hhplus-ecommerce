package kr.hhplus.be.server.order.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.exception.AlreadyPaidOrderException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderItemCommand
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.product.ProductTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderTest {
    
    @Test
    fun `✅주문 생성`() {
        // arrange
        val now = LocalDateTime.now()
        val order = Order.create(OrderCommand.Create(
            userId = 1L,
            products = listOf(ProductTestFixture.createValidProduct(1L), ProductTestFixture.createValidProduct(2L)),
            orderItems = listOf(OrderItemCommand.Create(1L, 1L, 1), OrderItemCommand.Create(1L, 2L, 1)), // 2600 포인트
            now = now
        ))

        // act, assert
        order.userId shouldBe 1L
        order.status shouldBe kr.hhplus.be.server.order.domain.OrderStatus.CREATED
        order.originalTotal shouldBe BigDecimal(2600)
        order.discountedAmount shouldBe BigDecimal.ZERO
        order.orderItems.size shouldBe 2
        order.createdAt shouldBe now
        order.updatedAt shouldBe now
    }
    
    @Test
    fun `✅주문 최종 금액 계산_finalTotal 은 originalTotal - discountedTotal 이어야 한다`() {
        // arrange
        val now = LocalDateTime.now()
        val order = Order(
            id = 1L,
            userId = 1L,
            originalTotal = BigDecimal(100),
            discountedAmount = BigDecimal(50),
            createdAt = now,
            updatedAt = now
        )
        // act, assert
        order.finalTotal() shouldBe BigDecimal(50)
    }
    
    @Test
    fun `✅할인 항목 추가_할인 항목이 추가되면 해당 금액만큼 discountedAmount가 증가해야 한다`() {
        // arrange
        val now = LocalDateTime.now()
        val order = OrderTestFixture.createOrder(1L)
        val discountLines = listOf(
            DiscountLine(1L, 1L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 1L, BigDecimal(1000)),
            DiscountLine(2L, 2L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 2L, BigDecimal(1000)),
        )
        // act
        order.applyDiscount(discountLines)
        
        // assert
        order.discountedAmount shouldBe BigDecimal(2000)
    }

    @Test
    fun `✅할인 항목 추가_할인 항목이 추가되면 해당 금액만큼 orderItem의 discountedAmount가 증가해야 한다`() {
        // arrange
        val order = OrderTestFixture.createOrder(1L)
        val discountLines = listOf(
            DiscountLine(1L, orderItemId = 1L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 1L, BigDecimal(1000)),
            DiscountLine(2L, orderItemId = 2L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 2L, BigDecimal(1000)),
        )
        // act
        order.applyDiscount(discountLines)

        // assert
        order.orderItems[0].discountAmount shouldBe BigDecimal(1000)
        order.orderItems[1].discountAmount shouldBe BigDecimal(1000)
    }

    @Test
    fun `✅할인 항목 추가 싪패_할인 항목이 추가됐을 때 해당하는 orderItem이 없으면 IllegalStateException 예외를 발생시켜야 한다`() {
        // arrange
        val order = OrderTestFixture.createOrder(1L)
        val discountLines = listOf(
            DiscountLine(1L, orderItemId = 3L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 1L, BigDecimal(1000)),
            DiscountLine(2L, orderItemId = 4L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 2L, BigDecimal(1000)),
        )
        // act, assert
        shouldThrowExactly<IllegalStateException> { order.applyDiscount(discountLines) }
    }

    @Test
    fun `⛔️할인 항목 추가_할인의 총합은 주문 금액 총합을 넘을 수 없다`() {
        // arrange
        val order = OrderTestFixture.createOrder(1L)
        val discountLines = listOf(
            DiscountLine(1L, 1L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 1L, BigDecimal(10000)),
            DiscountLine(2L, 2L, kr.hhplus.be.server.coupon.domain.model.DiscountMethod.COUPON, 2L, BigDecimal(10000)),
        )
        // act
        order.applyDiscount(discountLines)

        // assert
        order.discountedAmount shouldBe BigDecimal(10000)
    }
    
    @Test
    fun `✅주문 결제 완료_상태가 PAID로 변경되어야 한다`() {
        // arrange
        val order = Order(
            id = 1L,
            userId = 1L
        )
        // act
        order.completeOrder()
        
        // assert
        order.status shouldBe kr.hhplus.be.server.order.domain.OrderStatus.PAID
    }


    @Test
    fun `⛔️주문 결제 실패_이미 결제된 상품에 대해서는 실패해야 한다`() {
        // arrange
        val order = Order(
            id = 1L,
            userId = 1L,
            status = kr.hhplus.be.server.order.domain.OrderStatus.PAID
        )
        // act, assert
        val ex = shouldThrowExactly<AlreadyPaidOrderException> { order.completeOrder() }
        ex.message shouldBe ErrorCode.ALREADY_PAID_ORDER.message
    }
}