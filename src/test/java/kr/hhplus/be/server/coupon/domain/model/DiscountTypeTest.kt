package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.OrderTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountTypeTest {

    @Test
    fun `✅정액 할인(전체 할인)_주문 금액이 할인 금액보다 큰 경우 할인 금액을 각 상품의 금액의 비율로 나누어 반환한다`() {
        // arrange
        val order = OrderTestFixture.createOrder()
        val targetItems = order.orderItems
        val discountAmount = BigDecimal(500)
        val type = FixedAmountTotalDiscountType(discountAmount)
        // act
        val orderItemDiscountMap = type.calculateDiscount(order, targetItems)
        // assert
        val entries = orderItemDiscountMap.entries.toList()
        entries[0].value.compareTo(BigDecimal(250)) shouldBe 0
        entries[1].value.compareTo(BigDecimal(250)) shouldBe 0
    }

    @Test
    fun `✅정액 할인(전체 할인)_할인 금액이 주문 금액보다 큰 경우 주문 금액을 각 상품의 금액의 비율로 나누어 반환한다`() {
        // arrange
        val order = OrderTestFixture.createOrder()
        val discountAmount = BigDecimal(20000)
        val type = FixedAmountTotalDiscountType(discountAmount)
        // act
        val orderItemDiscountMap = type.calculateDiscount(order, order.orderItems)
        // assert
        val discountAmounts = orderItemDiscountMap.values.toList()
        discountAmounts[0].compareTo(BigDecimal(5000)) shouldBe 0
        discountAmounts[1].compareTo(BigDecimal(5000)) shouldBe 0
        discountAmounts.sumOf { it }.compareTo(BigDecimal(10000))
    }


    @Test
    fun `✅정액 할인(상품별 할인)_주문 금액이 할인 금액보다 큰 경우 할인 금액을 반환한다`() {
        // arrange
        val order = OrderTestFixture.createOrder()
        val targetItems = order.orderItems
        val discountAmount = BigDecimal(500)
        val type = FixedAmountPerItemDiscountType(discountAmount)
        // act
        val orderItemDiscountMap = type.calculateDiscount(order, targetItems)
        // assert
        val entries = orderItemDiscountMap.entries.toList()
        entries[0].value.compareTo(BigDecimal(500)) shouldBe 0
        entries[1].value.compareTo(BigDecimal(500)) shouldBe 0
    }

    @Test
    fun `✅정액 할인(전체 할인)_할인 금액이 주문 금액보다 큰 경우 주문 금액을 반환한다`() {
        // arrange
        val order = OrderTestFixture.createOrder()
        val discountAmount = BigDecimal(20000)
        val type = FixedAmountPerItemDiscountType(discountAmount)
        // act
        val orderItemDiscountMap = type.calculateDiscount(order, order.orderItems)
        // assert
        val discountAmounts = orderItemDiscountMap.values.toList()
        discountAmounts[0].compareTo(BigDecimal(5000)) shouldBe 0
        discountAmounts[1].compareTo(BigDecimal(5000)) shouldBe 0
        discountAmounts.sumOf { it }.compareTo(BigDecimal(10000))
    }



    @Test
    fun `✅정률 할인`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val order = OrderTestFixture.createOrder()
        val type = RateDiscountType(discountRate)
        // act
        val orderItemsDiscountMap = type.calculateDiscount(order, order.orderItems)
        val discounts = orderItemsDiscountMap.values.toList()
        // assert
        discounts[0].compareTo(BigDecimal(2500)) shouldBe 0
        discounts[1].compareTo(BigDecimal(2500)) shouldBe 0
    }

    @Test
    fun `✅정률 할인_최대 할인금액까지만 할인받을 수 있다`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val maxDiscountAmount = BigDecimal(1000)
        val order = OrderTestFixture.createOrder(
        )
        val type = RateDiscountType(discountRate, maxDiscountAmount)
        // act
        val orderItemsDiscountMap = type.calculateDiscount(order, order.orderItems)
        val discounts = orderItemsDiscountMap.values.toList()
        // assert
        discounts[0].compareTo(BigDecimal(500)) shouldBe 0
        discounts[1].compareTo(BigDecimal(500)) shouldBe 0
    }
}