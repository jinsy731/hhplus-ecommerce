package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.coupon.CouponTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountTypeTest {

    @Test
    fun `✅정액 할인(전체 할인)_주문 금액이 할인 금액보다 큰 경우 할인 금액을 각 상품의 금액의 비율로 나누어 반환한다`() {
        // arrange
        val discountAmount = Money.of(500)
        val type = FixedAmountTotalDiscountType(discountAmount)
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemDiscountMap = type.calculateDiscount(context)
        // assert
        val entries = orderItemDiscountMap.entries.toList()
        entries[0].value.compareTo(Money.of(250)) shouldBe 0
        entries[1].value.compareTo(Money.of(250)) shouldBe 0
    }

    @Test
    fun `✅정액 할인(전체 할인)_할인 금액이 주문 금액보다 큰 경우 주문 금액을 각 상품의 금액의 비율로 나누어 반환한다`() {
        // arrange
        val discountAmount = Money.of(30000)
        val type = FixedAmountTotalDiscountType(discountAmount)
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemDiscountMap = type.calculateDiscount(context)
        // assert
        val discountAmounts = orderItemDiscountMap.values.toList()
        discountAmounts[0].compareTo(Money.of(10000)) shouldBe 0
        discountAmounts[1].compareTo(Money.of(10000)) shouldBe 0
        discountAmounts.sumOf { it.amount }.compareTo(BigDecimal(20000))
    }


    @Test
    fun `✅정액 할인(상품별 할인)_주문 금액이 할인 금액보다 큰 경우 할인 금액을 반환한다`() {
        // arrange
        val discountAmount = Money.of(500)
        val type = FixedAmountPerItemDiscountType(discountAmount)
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemDiscountMap = type.calculateDiscount(context)
        // assert
        val entries = orderItemDiscountMap.entries.toList()
        entries[0].value.compareTo(Money.of(500)) shouldBe 0
        entries[1].value.compareTo(Money.of(500)) shouldBe 0
    }

    @Test
    fun `✅정액 할인(상품별 할인)_할인 금액이 주문 금액보다 큰 경우 주문 금액을 반환한다`() {
        // arrange
        val discountAmount = Money.of(20000)
        val type = FixedAmountPerItemDiscountType(discountAmount)
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemDiscountMap = type.calculateDiscount(context)
        // assert
        val discountAmounts = orderItemDiscountMap.values.toList()
        discountAmounts[0].compareTo(Money.of(10000)) shouldBe 0
        discountAmounts[1].compareTo(Money.of(10000)) shouldBe 0
        discountAmounts.sumOf { it.amount }.compareTo(BigDecimal(20000))
    }



    @Test
    fun `✅정률 할인`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val type = RateDiscountType(discountRate)
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemsDiscountMap = type.calculateDiscount(context)
        val discounts = orderItemsDiscountMap.values.toList()
        // assert
        discounts[0] shouldBe Money.of(5000)
        discounts[1] shouldBe Money.of(5000)
    }

    @Test
    fun `✅정률 할인_최대 할인금액까지만 할인받을 수 있다`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val maxDiscountAmount = Money.of(1000)
        val context = CouponTestFixture.createDiscountContext()
        val type = RateDiscountType(discountRate, maxDiscountAmount)
        // act
        val orderItemsDiscountMap = type.calculateDiscount(context)
        val discounts = orderItemsDiscountMap.values.toList()
        // assert
        discounts[0] shouldBe Money.of(500)
        discounts[1] shouldBe Money.of(500)
    }
}