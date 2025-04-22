package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AllProductConditionTest {

    @Test
    fun `모든 상품에 대해 할인이 적용되어야 한다`() {
        // arrange
        val condition = AllProductCondition()
        // act
        val result = condition.isSatisfiedBy(DiscountContext.Item(
            orderItemId = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            subTotal = Money.of(100),
            totalAmount = Money.of(100)
        ))
        // assert
        result shouldBe true
    }
}