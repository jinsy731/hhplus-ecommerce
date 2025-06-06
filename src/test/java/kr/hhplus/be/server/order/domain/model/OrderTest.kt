package kr.hhplus.be.server.order.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.AlreadyPaidOrderException
import kr.hhplus.be.server.shared.exception.ErrorCode
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OrderTest {
    
    @Test
    fun `✅주문 생성`() {
        // arrange
        val now = LocalDateTime.now()
        val order = Order.create(OrderContext.Create.Root(
            userId = 1L,
            timestamp = now,
            items = listOf(OrderContext.Create.Item(
                productId = 1L,
                variantId = 1L,
                quantity = 1,
                unitPrice = Money.of(1000)
            ))
        ))

        // act, assert
        order.userId shouldBe 1L
        order.status shouldBe OrderStatus.CREATED
        order.originalTotal shouldBe Money.of(1000)
        order.discountedAmount shouldBe Money.ZERO
        order.orderItems.size() shouldBe 1
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
            originalTotal = Money.of(100),
            discountedAmount = Money.of(50),
            createdAt = now,
            updatedAt = now
        )
        // act, assert
        order.finalTotal() shouldBe Money.of(50)
    }
    
    @Test
    fun `✅할인 항목 추가_할인 항목이 추가되면 해당 금액만큼 discountedAmount가 증가해야 한다`() {
        // arrange
        val order = OrderTestFixture.order(userId = 1L).withStandardItems(true).build()
        val discountLines = listOf(
            DiscountInfo(
                orderItemId = 1L,
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON"),
            DiscountInfo(
                orderItemId = 2L,
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON")
        )
        // act
        order.applyDiscount(discountLines)
        
        // assert
        order.discountedAmount shouldBe Money.of(2000)
    }

    @Test
    fun `✅할인 항목 추가_할인 항목이 추가되면 해당 금액만큼 orderItem의 discountedAmount가 증가해야 한다`() {
        // arrange
        val order = OrderTestFixture.order(userId = 1L)
            .withStandardItems(true).build()
        val orderItems = order.orderItems.asList()
        val discountLines = listOf(
            DiscountInfo(
                orderItemId = 1L,
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON"),
            DiscountInfo(
                orderItemId = 2L,
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON")
        )
        // act
        order.applyDiscount(discountLines)

        // assert
        orderItems[0].discountAmount shouldBe Money.of(1000)
        orderItems[1].discountAmount shouldBe Money.of(1000)
    }

    @Test
    fun `✅할인 항목 추가 싪패_할인 항목이 추가됐을 때 해당하는 orderItem이 없으면 IllegalStateException 예외를 발생시켜야 한다`() {
        // arrange
        val order = OrderTestFixture.standardOrder(userId = 1L)
        val discountLines = listOf(
            DiscountInfo(
                orderItemId = 3L, // 존재하지 않는 orderItem ID
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON"),
            DiscountInfo(
                orderItemId = 4L, // 존재하지 않는 orderItem ID
                amount = Money.of(1000),
                sourceId = 1L,
                sourceType = "COUPON")
        )
        // act, assert
        shouldThrowExactly<IllegalStateException> { order.applyDiscount(discountLines) }
    }

    @Test
    fun `⛔️할인 항목 추가_할인의 총합은 주문 금액 총합을 넘을 수 없다`() {
        // arrange
        val order = OrderTestFixture.order(userId = 1L)
            .withOriginalTotal(Money.of(10000))
            .withStandardItems(true)
            .build()
            
        val discountLines = listOf(
            DiscountInfo(
                orderItemId = 1L,
                amount = Money.of(100000), // 주문 금액보다 큰 할인 금액
                sourceId = 1L,
                sourceType = "COUPON"),
            DiscountInfo(
                orderItemId = 2L,
                amount = Money.of(100000), // 주문 금액보다 큰 할인 금액
                sourceId = 1L,
                sourceType = "COUPON")
        )
        // act
        order.applyDiscount(discountLines)

        // assert
        order.discountedAmount shouldBe Money.of(10000) // 최대 주문 금액까지만 할인됨
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
        order.status shouldBe OrderStatus.PAID
    }


    @Test
    fun `⛔️주문 결제 실패_이미 결제된 상품에 대해서는 실패해야 한다`() {
        // arrange
        val order = Order(
            id = 1L,
            userId = 1L,
            status = OrderStatus.PAID
        )
        // act, assert
        val ex = shouldThrowExactly<AlreadyPaidOrderException> { order.completeOrder() }
        ex.message shouldBe ErrorCode.ALREADY_PAID_ORDER.message
    }
}