package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountTypeTest {

    @Test
    fun `✅정액 할인_주문 금액이 할인 금액보다 큰 경우 할인 금액을 반환한다`() {
        // arrange
        val discountAmount = BigDecimal(500)
        val price = BigDecimal(2000)
        val type = FixedAmountDiscountType(discountAmount)
        // act
        val discount = type.calculateDiscount(price)
        // assert
        discount shouldBe BigDecimal(500)
    }

    @Test
    fun `✅정액 할인_할인 금액이 주문 금액보다 큰 경우 주문 금액을 반환한다`() {
        // arrange
        val discountAmount = BigDecimal(2000)
        val price = BigDecimal(500)
        val type = FixedAmountDiscountType(discountAmount)
        // act
        val discount = type.calculateDiscount(price)
        // assert
        discount shouldBe BigDecimal(500)
    }

    @Test
    fun `✅정률 할인`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val price = BigDecimal(500)
        val type = RateDiscountType(discountRate)
        // act
        val discount = type.calculateDiscount(price)
        // assert
        discount.compareTo(BigDecimal(250)) shouldBe 0
    }

    @Test
    fun `✅정률 할인_최대 할인금액까지만 할인받을 수 있다`() {
        // arrange
        val discountRate = BigDecimal(0.5)
        val maxDiscountAmount = BigDecimal(100)
        val price = BigDecimal(500)
        val type = RateDiscountType(discountRate, maxDiscountAmount)
        // act
        val discount = type.calculateDiscount(price)
        // assert
        discount.compareTo(BigDecimal(100)) shouldBe 0
    }
}