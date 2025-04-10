package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import kr.hhplus.be.server.coupon.domain.model.FixedAmountDiscountType
import kr.hhplus.be.server.coupon.domain.model.MinOrderAmountCondition
import java.math.BigDecimal
import java.time.LocalDateTime

object CouponTestFixture {

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
        discountType = FixedAmountDiscountType(BigDecimal(5000)),
        discountCondition = MinOrderAmountCondition(BigDecimal(10000))
    )
}