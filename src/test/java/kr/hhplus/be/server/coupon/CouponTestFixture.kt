package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import kr.hhplus.be.server.coupon.domain.model.FixedAmountTotalDiscountType
import kr.hhplus.be.server.coupon.domain.model.MinOrderAmountCondition
import java.math.BigDecimal
import java.time.LocalDateTime

object CouponTestFixture {

    /**
     * 1. 할인 금액 5000원
     * 2. 할인 조건 10000원 이상 구매
     */
    fun createValidCoupon(
        startAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1)) = Coupon(
        id = 1L,
        name = "쿠폰 A",
        description = "신규 할인 쿠폰",
        discountPolicy = createFixedAmountDiscountPolicy(),
        isActive = true,
        maxIssueLimit = 10,
        issuedCount = 0,
        startAt = startAt,
        endAt = endAt,
        validDays = 10,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun createInvalidCoupon() = Coupon(
        id = 1L,
        name = "쿠폰 A",
        description = "신규 할인 쿠폰",
        discountPolicy = createFixedAmountDiscountPolicy(),
        isActive = false,
        maxIssueLimit = 10,
        issuedCount = 0,
        startAt = LocalDateTime.now(),
        endAt = LocalDateTime.now().plusHours(1),
        validDays = 10,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun createFixedAmountDiscountPolicy() = DiscountPolicy(
        name = "5000원 정액 할인",
        discountType = FixedAmountTotalDiscountType(BigDecimal(5000)),
        discountCondition = MinOrderAmountCondition(BigDecimal(10000))
    )
}