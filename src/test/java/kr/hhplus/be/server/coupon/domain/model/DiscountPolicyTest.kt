package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.coupon.CouponTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountPolicyTest {
    
    @Test
    fun `✅할인 금액 계산_조건을 만족하면 할인 금액을 반환해야 한다`() {
        // arrange
        val policy = DiscountPolicy(
            name = "할인 정책 A",
            discountType = FixedAmountTotalDiscountType(Money.of(1000)),
            discountCondition = MinOrderAmountCondition(Money.of(100))
        )
        val context = CouponTestFixture.createDiscountContext()
        // act
        val orderItemsDiscountMap = policy.calculateDiscount(context)
        // assert
        val discounts = orderItemsDiscountMap.values
            .fold(Money.ZERO) { acc, it -> acc + it }
            .amount

        discounts.compareTo(BigDecimal(1000)) shouldBe 0
    }
}