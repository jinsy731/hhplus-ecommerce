package kr.hhplus.be.server.coupon.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.CouponTargetNotFoundException
import kr.hhplus.be.server.shared.exception.ExceededMaxCouponLimitException
import kr.hhplus.be.server.shared.exception.InvalidCouponStatusException
import kr.hhplus.be.server.coupon.CouponTestFixture
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CouponTest {
    
    @Test
    fun `✅쿠폰 유효성 검증_isActive가 true이고 현재 시각이 startAt과 endAt 사이이면 예외를 발생시키지 않는다`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        // act, assert
        shouldNotThrowAny { coupon.validatUsability(LocalDateTime.now()) }
    }

    @Test
    fun `⛔️쿠폰 유효성 검증_isActive가 false이거나 현재 시각이 startAt과 endAt 사이가 아니면 InvalidCouponException 예외를 발생시킨다`() {
        // arrange
        val coupon1 = CouponTestFixture.createInvalidCoupon(isActive = false)
        val coupon2 = CouponTestFixture.createInvalidCoupon(
            isActive = true,
            startAt = LocalDateTime.now().minusHours(2),
            endAt = LocalDateTime.now().minusHours(1),)

        // act, assert
        shouldThrowExactly<InvalidCouponStatusException> { coupon1.validatUsability(LocalDateTime.now()) }
        shouldThrowExactly<InvalidCouponStatusException> { coupon2.validatUsability(LocalDateTime.now()) }
    }


    @Test
    fun `✅할인 금액 계산_쿠폰이 유효하면 할인 금액을 반환해야 한다`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon(discountPolicy = DiscountPolicy(
            name = "",
            discountType = FixedAmountTotalDiscountType(Money.of(10000)),
            discountCondition = MinOrderAmountCondition(minAmount = Money.of(5000))
        ))
        val context = CouponTestFixture.createDiscountContext()
        val applicableItems = listOf(1L, 2L)
        // act
        val orderItemDiscountMap = coupon.calculateDiscount(context, applicableItems)
        // assert
        val discountAmounts = orderItemDiscountMap.values.toList()
        discountAmounts[0].compareTo(Money.of(5000)) shouldBe 0
        discountAmounts[1].compareTo(Money.of(5000)) shouldBe 0
    }

    @Test
    fun `✅할인 적용 대상 반환`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val context = DiscountContext.Root(
            items = listOf(
                DiscountContext.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(10000),
                    totalAmount = Money.of(20000)
                ),
                DiscountContext.Item(
                    orderItemId = 2L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(10000),
                    totalAmount = Money.of(20000)
                )
            ))
        // act
        val result = coupon.getApplicableItems(context)
        // assert
        result shouldHaveSize 2
    }

    @Test
    fun `⛔️할인 적용 대상 반환_적용할 수 있는 대상이 없으면 CouponTargetNotFoundException 예외를 발생시킨다`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val context = DiscountContext.Root(
            items = listOf(
                DiscountContext.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(100),
                    totalAmount = Money.of(200)
                ),
                DiscountContext.Item(
                    orderItemId = 2L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(100),
                    totalAmount = Money.of(200)
                )
            ))
        // act, assert
        shouldThrowExactly<CouponTargetNotFoundException> { coupon.getApplicableItems(context) }
    }

    @Test
    fun `✅쿠폰 발급`() {
        // arrange
        val coupon = CouponTestFixture.createValidCoupon()
        val now = LocalDateTime.now()
        // act
        val userCoupon = coupon.issueTo(1L, now)
        // assert
        coupon.issuedCount shouldBe 1
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