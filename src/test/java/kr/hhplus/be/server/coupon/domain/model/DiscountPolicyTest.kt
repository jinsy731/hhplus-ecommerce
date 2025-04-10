package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountPolicyTest {
    
    @Test
    fun `✅할인 금액 계산_조건을 만족하면 할인 금액을 반환해야 한다`() {
        // arrange
        val policy = DiscountPolicy(
            name = "할인 정책 A",
            discountType = FixedAmountDiscountType(BigDecimal(1000)),
            discountCondition = MinOrderAmountCondition(BigDecimal(100))
        )
        // act
        val discountAmount = policy.calculateDiscount(price = BigDecimal(500))
        // assert
        discountAmount.compareTo(BigDecimal(500)) shouldBe 0
    }
}