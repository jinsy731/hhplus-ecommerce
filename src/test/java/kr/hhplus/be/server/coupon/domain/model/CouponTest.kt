package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.exception.ExceededMaxCouponLimitException
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.order.OrderTestFixture
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponTest {
    
    @Test
    fun `✅쿠폰 유효성 검증_isActive가 true이고 현재 시각이 startAt과 endAt 사이이면 true를 반환해야 한다`() {
        // arrange
        val coupon = Coupon(
            id = 1L,
            name = "쿠폰 A",
            description = "신규 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = LocalDateTime.now(),
            endAt = LocalDateTime.now().plusHours(1),
            validDays = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        // act
        val result = coupon.isValid(LocalDateTime.now())
        // assert
        result shouldBe true
    }

    @Test
    fun `⛔️쿠폰 유효성 검증_isActive가 false이거나 현재 시각이 startAt과 endAt 사이가 아니면 false를 반환해야 한다`() {
        // arrange
        val coupon1 = Coupon(
            id = 1L,
            name = "쿠폰 A",
            description = "신규 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = false,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = LocalDateTime.now(),
            endAt = LocalDateTime.now().plusHours(1),
            validDays = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val coupon2 = Coupon(
            id = 1L,
            name = "쿠폰 A",
            description = "신규 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = LocalDateTime.now().minusHours(2),
            endAt = LocalDateTime.now().minusHours(1),
            validDays = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        // act
        val result1 = coupon1.isValid(LocalDateTime.now())
        val result2 = coupon2.isValid(LocalDateTime.now())
        // assert
        result1 shouldBe false
        result2 shouldBe false
    }


    @Test
    fun `✅할인 금액 계산_쿠폰이 유효하면 할인 금액을 반환해야 한다`() {
        // arrange
        val now = LocalDateTime.now()
        val coupon = Coupon(
            id = 1L,
            name = "쿠폰 A",
            description = "신규 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = LocalDateTime.now().minusMinutes(1),
            endAt = LocalDateTime.now().plusHours(1),
            validDays = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        // act
        val discountAmount = coupon.calculateDiscount(
            now = now,
            price = BigDecimal(20000))
        println("discountAmount = ${discountAmount}")
        // assert
        discountAmount.compareTo(BigDecimal(5000)) shouldBe 0
    }

    @Test
    fun `⛔️할인 금액 계산_쿠폰이 유효하지 않으면 0을 반환해야 한다`() {
        // arrange
        val now = LocalDateTime.now()
        val coupon = Coupon(
            id = 1L,
            name = "쿠폰 A",
            description = "신규 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = false,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = LocalDateTime.now(),
            endAt = LocalDateTime.now().plusHours(1),
            validDays = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        // act
        val discountAmount = coupon.calculateDiscount(
            now = now,
            price = BigDecimal(20000))
        // assert
        discountAmount.compareTo(BigDecimal(0)) shouldBe 0
    }

    @Test
    fun `✅할인 적용 대상 반환`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val order = OrderTestFixture.createOrder(1L)
        // act
        val result = coupon.applicableItems(order, 1L)
        // assert
        result shouldHaveSize 2
    }

    @Test
    fun `⛔️할인 적용 대상 반환_적용할 수 있는 대상이 없으면 빈 리스트를 반환한다`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val order = OrderTestFixture.createOrder(1L).apply { this.originalTotal = BigDecimal(1000) }
        // act
        val result = coupon.applicableItems(order, 1L)
        // assert
        result shouldHaveSize 0
    }

    @Test
    fun `✅쿠폰 발급`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val now = LocalDateTime.now()
        // act
        val userCoupon = coupon.issueTo(1L, now)
        // assert
        userCoupon.userId shouldBe 1L
        userCoupon.status shouldBe UserCouponStatus.UNUSED
        userCoupon.expiredAt shouldBe now.plusDays(10)
    }

    @Test
    fun `⛔️쿠폰 발급 실패_최대 발급 가능한 수량을 초과하면 ExceededMaxCouponLimitException 예외를 발생시켜야 한다`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon().apply {
            this.maxIssueLimit = 10
            this.issuedCount = 10
        }
        val now = LocalDateTime.now()
        // act, assert
        shouldThrowExactly<ExceededMaxCouponLimitException> { coupon.issueTo(1L, now) }
    }
}