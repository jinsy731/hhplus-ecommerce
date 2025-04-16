package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MinOrderAmountConditionTest {
    
    @Test
    fun `✅쿠폰 적용 가능_주문 금액이 최소 주문 금액 이상이면 true를 반환해야 한다`() {
        // arrange
        val minAmount = BigDecimal(1000)
        val condition = MinOrderAmountCondition(minAmount)
        val context = DiscountContext(orderAmount = BigDecimal(1000))
        // act
        val result = condition.isSatisfiedBy(context)
        // assert
        result shouldBe true
    }

    @Test
    fun `⛔️쿠폰 적용 불가능_주문 금액이 최소 주문 금액 미만이면 false를 반환해야 한다`() {
        // arrange
        val minAmount = BigDecimal(1000)
        val condition = MinOrderAmountCondition(minAmount)
        val context = DiscountContext(orderAmount = BigDecimal(999))
        // act
        val result = condition.isSatisfiedBy(context)
        // assert
        result shouldBe false
    }
}