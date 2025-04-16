package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CompositeConditionTest {

    @Test
    fun `✅OR 컨디션 조합_조건 중 하나라도 일치하면 true를 반환한다`() {
        // arrange
        val minOrderAmountCondition = MinOrderAmountCondition(BigDecimal(1000))
        val specificProductCondition = SpecificProductCondition(setOf(1L, 2L))
        val contextItem = DiscountContext.Item(
            orderItemId = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            subTotal = BigDecimal(999),
            totalAmount = BigDecimal(999)
        )

        val compositeCondition = OrCompositeCondition()
            .addCondition(minOrderAmountCondition)
            .addCondition(specificProductCondition)

        // act
        val result = compositeCondition.isSatisfiedBy(contextItem)

        // assert
        result shouldBe true
    }

    @Test
    fun `⛔️OR 컨디션 조합_조건이 모두 일치하지 않으면 false를 반환한다`() {
        // arrange
        val minOrderAmountCondition = MinOrderAmountCondition(BigDecimal(1000))
        val specificProductCondition = SpecificProductCondition(setOf(1L, 2L))
        val contextItem = DiscountContext.Item(
            orderItemId = 1L,
            productId = 3L,
            variantId = 1L,
            quantity = 1,
            subTotal = BigDecimal(999),
            totalAmount = BigDecimal(999)
        )
        val compositeCondition = OrCompositeCondition()
            .addCondition(minOrderAmountCondition)
            .addCondition(specificProductCondition)

        // act
        val result = compositeCondition.isSatisfiedBy(contextItem)

        // assert
        result shouldBe false
    }

    @Test
    fun `⛔️AND 컨디션 조합_조건 중 하나라도 일치하지 않으면 false를 반환한다`() {
        // arrange
        val minOrderAmountCondition = MinOrderAmountCondition(BigDecimal(1000))
        val specificProductCondition = SpecificProductCondition(setOf(1L, 2L))
        val contextItem = DiscountContext.Item(
            orderItemId = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            subTotal = BigDecimal(999),
            totalAmount = BigDecimal(999)
        )
        val compositeCondition = AndCompositeCondition()
            .addCondition(minOrderAmountCondition)
            .addCondition(specificProductCondition)

        // act
        val result = compositeCondition.isSatisfiedBy(contextItem)

        // assert
        result shouldBe false
    }
    @Test
    fun `✅AND 컨디션 조합_조건이 모두 일치하면 true를 반환한다`() {
        // arrange
        val minOrderAmountCondition = MinOrderAmountCondition(BigDecimal(1000))
        val specificProductCondition = SpecificProductCondition(setOf(1L, 2L))
        val contextItem = DiscountContext.Item(
            orderItemId = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            subTotal = BigDecimal(1000),
            totalAmount = BigDecimal(1000)
        )
        val compositeCondition = AndCompositeCondition()
            .addCondition(minOrderAmountCondition)
            .addCondition(specificProductCondition)

        // act
        val result = compositeCondition.isSatisfiedBy(contextItem)

        // assert
        result shouldBe true
    }
}