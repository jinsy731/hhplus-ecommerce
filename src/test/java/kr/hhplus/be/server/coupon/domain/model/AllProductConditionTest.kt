package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AllProductConditionTest {

    @Test
    fun `모든 상품에 대해 할인이 적용되어야 한다`() {
        // arrange
        val condition = AllProductCondition()
        // act
        val result = condition.isSatisfiedBy(DiscountContext())
        // assert
        result shouldBe true
    }
}