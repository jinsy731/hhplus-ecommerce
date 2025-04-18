package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountCondition
import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import kr.hhplus.be.server.coupon.domain.model.DiscountType
import kr.hhplus.be.server.coupon.domain.model.FixedAmountTotalDiscountType
import kr.hhplus.be.server.coupon.domain.model.MinOrderAmountCondition
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

object CouponTestFixture {

    /**
     * 1. 할인 금액 5000원
     * 2. 할인 조건 10000원 이상 구매
     */
    fun createValidCoupon(
        id: Long? = null,
        startAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1),
        discountPolicy: DiscountPolicy = createFixedAmountDiscountPolicy()
        ) = Coupon(
        id = id ,
        name = "쿠폰 A",
        description = "신규 할인 쿠폰",
        discountPolicy = discountPolicy,
        isActive = true,
        maxIssueLimit = 10,
        issuedCount = 0,
        startAt = startAt,
        endAt = endAt,
        validDays = 10,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun createInvalidCoupon(
        id: Long? = null,
        isActive: Boolean = false,
        startAt: LocalDateTime = LocalDateTime.now(),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1)) = Coupon(
        id = id,
        name = "쿠폰 A",
        description = "신규 할인 쿠폰",
        discountPolicy = createFixedAmountDiscountPolicy(),
        isActive = isActive,
        maxIssueLimit = 10,
        issuedCount = 0,
        startAt = startAt,
        endAt = endAt,
        validDays = 10,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun createFixedAmountDiscountPolicy() = DiscountPolicy(
        name = "5000원 정액 할인",
        discountType = FixedAmountTotalDiscountType(BigDecimal(5000)),
        discountCondition = MinOrderAmountCondition(BigDecimal(10000))
    )


    fun createDiscountContext(
        subTotal1: BigDecimal = BigDecimal(10000),
        subTotal2: BigDecimal = BigDecimal(10000),
        timestamp: LocalDateTime = LocalDateTime.now()
        ) = DiscountContext.Root(
        items = listOf(
            createDiscountContextItem(subTotal = subTotal1, totalAmount = subTotal1 + subTotal2),
            createDiscountContextItem(
                orderItemId = 2L,
                variantId = 2L,
                subTotal = subTotal2,
                totalAmount = subTotal1 + subTotal2
            ),
        ),
        timestamp = timestamp,
    )

    fun createDiscountContextItem(
        orderItemId: Long = 1L,
        productId: Long = 1L,
        variantId: Long = 1L,
        quantity: Int = 1,
        subTotal: BigDecimal = BigDecimal(10000),
        totalAmount: BigDecimal = BigDecimal(20000),
        ) = DiscountContext.Item(
        orderItemId = orderItemId,
        productId = productId,
        variantId = variantId,
        quantity = quantity,
        subTotal = subTotal,
        totalAmount = totalAmount
    )

    fun createUserCoupon(
        id: Long? = null,
        userId: Long = 1L,
        coupon: Coupon
    ) = UserCoupon(
        id = id,
        userId = userId,
        coupon = coupon,
        issuedAt = LocalDateTime.now(),
        expiredAt = LocalDateTime.now().plusDays(7),
        usedAt = null,
        status = UserCouponStatus.UNUSED
    )
}