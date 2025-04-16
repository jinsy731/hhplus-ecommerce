package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.coupon.CouponTestFixture
import org.junit.jupiter.api.Test

class SpecificProductConditionTest {
    
    @Test
    fun `✅쿠폰 적용 가능_주문 상품 ID가 할인 가능 상품 ID에 포함되면 true를 반환해야 한다`() {
        // arrange
        val condition = SpecificProductCondition(setOf(1L, 2L, 3L))
        val contextItem = CouponTestFixture.createDiscountContextItem()
        // act
        val result = condition.isSatisfiedBy(contextItem)
        // assert
        result shouldBe true
    }

    @Test
    fun `⛔️쿠폰 적용 불가능_주문 상품 ID가 할인 가능 상품 ID에 포함되지 않으면 false를 반환해야 한다`() {
        // arrange
        val condition = SpecificProductCondition(setOf(2L, 3L))
        val contextItem = CouponTestFixture.createDiscountContextItem()
        // act
        val result = condition.isSatisfiedBy(contextItem)
        // assert
        result shouldBe false
    }
}