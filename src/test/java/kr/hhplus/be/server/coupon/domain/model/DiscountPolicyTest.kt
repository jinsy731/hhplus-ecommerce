package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.OrderTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DiscountPolicyTest {
    
    @Test
    fun `✅할인 금액 계산_조건을 만족하면 할인 금액을 반환해야 한다`() {
        // arrange
        val policy = DiscountPolicy(
            name = "할인 정책 A",
            discountType = FixedAmountTotalDiscountType(BigDecimal(1000)),
            discountCondition = MinOrderAmountCondition(BigDecimal(100))
        )
        val order = OrderTestFixture.createOrder()
        // act
        val orderItemsDiscountMap = policy.calculateDiscount(order, order.orderItems)
        val discounts = orderItemsDiscountMap.values.toList()
        // assert
        discounts.sumOf { it }.compareTo(BigDecimal(1000)) shouldBe 0
    }
}