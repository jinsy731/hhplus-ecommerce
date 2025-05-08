package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.coupon.CouponTestFixture
import org.junit.jupiter.api.Test

class MinOrderAmountConditionTest {
    
    @Test
    fun `✅쿠폰 적용 가능_주문 금액이 최소 주문 금액 이상이면 true를 반환해야 한다`() {
        // arrange
        val minAmount = Money.of(1000)
        val condition = MinOrderAmountCondition(minAmount)
        val contextItem = CouponTestFixture.createDiscountContextItem()

        // act
        val result = condition.isSatisfiedBy(contextItem)
        // assert
        result shouldBe true
    }

    @Test
    fun `⛔️쿠폰 적용 불가능_주문 금액이 최소 주문 금액 미만이면 false를 반환해야 한다`() {
        // arrange
        val minAmount = Money.of(1000)
        val condition = MinOrderAmountCondition(minAmount)
        val contextItem = CouponTestFixture.createDiscountContextItem(totalAmount = Money.of(999))
        // act
        val result = condition.isSatisfiedBy(contextItem)
        // assert
        result shouldBe false
    }
}