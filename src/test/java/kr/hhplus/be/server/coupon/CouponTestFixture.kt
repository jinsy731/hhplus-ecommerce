package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * 쿠폰 관련 테스트 픽스처
 * 테스트 의도에 맞게 쿠폰, 할인 정책, 할인 조건 등을 생성합니다.
 */
object CouponTestFixture {

    // 기본 쿠폰 생성 빌더
    fun coupon(
        id: Long? = null,
        name: String = "테스트 쿠폰",
        description: String = "테스트 쿠폰 설명",
        discountPolicy: DiscountPolicy = fixedAmountDiscountPolicy(),
        isActive: Boolean = true,
        maxIssueLimit: Int = 10,
        issuedCount: Int = 0,
        startAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1),
        validDays: Int = 10,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): CouponBuilder {
        return CouponBuilder(
            id = id,
            name = name,
            description = description,
            discountPolicy = discountPolicy,
            isActive = isActive,
            maxIssueLimit = maxIssueLimit,
            issuedCount = issuedCount,
            startAt = startAt,
            endAt = endAt,
            validDays = validDays,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // 할인 컨텍스트 빌더
    fun discountContext(
        items: List<DiscountContext.Item>? = null,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): DiscountContextBuilder {
        return DiscountContextBuilder(
            itemList = items ?: emptyList(),
            timestamp = timestamp
        )
    }

    // 유저 쿠폰 빌더
    fun userCoupon(
        id: Long? = null,
        userId: Long = 1L,
        coupon: Coupon,
        issuedAt: LocalDateTime = LocalDateTime.now(),
        expiredAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        usedAt: LocalDateTime? = null,
        status: UserCouponStatus = UserCouponStatus.UNUSED,
        orderId: Long? = null,
    ): UserCouponBuilder {
        return UserCouponBuilder(
            id = id,
            userId = userId,
            coupon = coupon,
            issuedAt = issuedAt,
            expiredAt = expiredAt,
            usedAt = usedAt,
            status = status,
            orderId = orderId
        )
    }

    // 시나리오별 쿠폰 생성 메서드

    /**
     * 유효한 정액 할인 쿠폰 (5000원 할인, 최소 주문금액 10000원)
     */
    fun validFixedAmountCoupon(
        id: Long? = null,
        startAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1)
    ): Coupon {
        return coupon(
            id = id,
            name = "5000원 할인 쿠폰",
            description = "10000원 이상 구매 시 5000원 할인",
            discountPolicy = fixedAmountDiscountPolicy(),
            startAt = startAt,
            endAt = endAt
        ).build()
    }

    /**
     * 유효하지 않은 쿠폰 (비활성화 상태)
     */
    fun inactiveCoupon(
        id: Long? = null,
        startAt: LocalDateTime = LocalDateTime.now(),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1)
    ): Coupon {
        return coupon(
            id = id,
            isActive = false,
            startAt = startAt,
            endAt = endAt
        ).build()
    }

    /**
     * 기간이 만료된 쿠폰
     */
    fun expiredCoupon(id: Long? = null): Coupon {
        return coupon(
            id = id,
            startAt = LocalDateTime.now().minusDays(5),
            endAt = LocalDateTime.now().minusDays(1)
        ).build()
    }

    /**
     * 정액 할인 정책 (5000원 할인, 최소 주문금액 10000원)
     */
    fun fixedAmountDiscountPolicy(
        discountAmount: Money = Money.of(5000),
        minOrderAmount: Money = Money.of(10000)
    ): DiscountPolicy {
        return DiscountPolicy(
            name = "${discountAmount.amount}원 정액 할인",
            discountType = FixedAmountTotalDiscountType(discountAmount),
            discountCondition = MinOrderAmountCondition(minOrderAmount)
        )
    }

    /**
     * 정률 할인 정책 (10% 할인, 최소 주문금액 10000원)
     */
    fun percentageDiscountPolicy(
        discountRate: Double = 0.1,
        minOrderAmount: Money = Money.of(10000)
    ): DiscountPolicy {
        return DiscountPolicy(
            name = "${discountRate * 100}% 정률 할인",
            discountType = RateDiscountType(discountRate.toBigDecimal()),
            discountCondition = MinOrderAmountCondition(minOrderAmount)
        )
    }

    fun noConditionDiscountPolicy() = DiscountPolicy(
        name = "TEST",
        discountType = FixedAmountTotalDiscountType(Money.of(1000)),
        discountCondition = allProductCondition()
    )

    /**
     * 특정 상품 할인 조건 (상품 ID 1번)
     */
    fun specificProductCondition(productId: Long = 1L): DiscountCondition {
        return SpecificProductCondition(setOf(productId))
    }

    /**
     * 모든 상품 할인 조건
     */
    fun allProductCondition(): DiscountCondition {
        return AllProductCondition()
    }

    /**
     * 주문 최소 금액 조건 (10000원 이상)
     */
    fun minOrderAmountCondition(minAmount: Money = Money.of(10000)): DiscountCondition {
        return MinOrderAmountCondition(minAmount)
    }

    /**
     * 할인 컨텍스트에 사용할 아이템 생성 (기본: 10000원 주문)
     */
    fun standardDiscountContextItem(
        orderItemId: Long = 1L,
        productId: Long = 1L,
        variantId: Long = 1L,
        quantity: Int = 1,
        subTotal: Money = Money.of(10000),
        totalAmount: Money = Money.of(10000)
    ): DiscountContext.Item {
        return DiscountContext.Item(
            orderItemId = orderItemId,
            productId = productId,
            variantId = variantId,
            quantity = quantity,
            subTotal = subTotal,
            totalAmount = totalAmount
        )
    }

    // 빌더 클래스들
    class CouponBuilder(
        private val id: Long?,
        private val name: String,
        private val description: String,
        private val discountPolicy: DiscountPolicy,
        private val isActive: Boolean,
        private val maxIssueLimit: Int,
        private val issuedCount: Int,
        private val startAt: LocalDateTime,
        private val endAt: LocalDateTime,
        private val validDays: Int,
        private val createdAt: LocalDateTime,
        private val updatedAt: LocalDateTime
    ) {
        fun build(): Coupon {
            return Coupon(
                id = id,
                name = name,
                description = description,
                discountPolicy = discountPolicy,
                isActive = isActive,
                maxIssueLimit = maxIssueLimit,
                issuedCount = issuedCount,
                startAt = startAt,
                endAt = endAt,
                validDays = validDays,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        fun withName(name: String): CouponBuilder {
            return CouponBuilder(
                id = id,
                name = name,
                description = description,
                discountPolicy = discountPolicy,
                isActive = isActive,
                maxIssueLimit = maxIssueLimit,
                issuedCount = issuedCount,
                startAt = startAt,
                endAt = endAt,
                validDays = validDays,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        fun withDiscountPolicy(discountPolicy: DiscountPolicy): CouponBuilder {
            return CouponBuilder(
                id = id,
                name = name,
                description = description,
                discountPolicy = discountPolicy,
                isActive = isActive,
                maxIssueLimit = maxIssueLimit,
                issuedCount = issuedCount,
                startAt = startAt,
                endAt = endAt,
                validDays = validDays,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        fun withActiveStatus(isActive: Boolean): CouponBuilder {
            return CouponBuilder(
                id = id,
                name = name,
                description = description,
                discountPolicy = discountPolicy,
                isActive = isActive,
                maxIssueLimit = maxIssueLimit,
                issuedCount = issuedCount,
                startAt = startAt,
                endAt = endAt,
                validDays = validDays,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        fun withValidPeriod(
            startAt: LocalDateTime = this.startAt,
            endAt: LocalDateTime = this.endAt
        ): CouponBuilder {
            return CouponBuilder(
                id = id,
                name = name,
                description = description,
                discountPolicy = discountPolicy,
                isActive = isActive,
                maxIssueLimit = maxIssueLimit,
                issuedCount = issuedCount,
                startAt = startAt,
                endAt = endAt,
                validDays = validDays,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }

    class DiscountContextBuilder(
        private val itemList: List<DiscountContext.Item>,
        private val timestamp: LocalDateTime
    ) {
        fun build(): DiscountContext.Root {
            return DiscountContext.Root(
                items = if (itemList.isEmpty()) {
                    listOf(standardDiscountContextItem())
                } else {
                    itemList
                },
                timestamp = timestamp
            )
        }

        fun withItems(vararg items: DiscountContext.Item): DiscountContextBuilder {
            return DiscountContextBuilder(
                itemList = items.toList(),
                timestamp = timestamp
            )
        }

        fun withTimestamp(timestamp: LocalDateTime): DiscountContextBuilder {
            return DiscountContextBuilder(
                itemList = itemList,
                timestamp = timestamp
            )
        }

        fun withTotalAmount(totalAmount: Money): DiscountContextBuilder {
            val updatedItems = itemList.map { item ->
                DiscountContext.Item(
                    orderItemId = item.orderItemId,
                    productId = item.productId,
                    variantId = item.variantId,
                    quantity = item.quantity,
                    subTotal = item.subTotal,
                    totalAmount = totalAmount
                )
            }

            return DiscountContextBuilder(
                itemList = updatedItems,
                timestamp = timestamp
            )
        }
    }

    class UserCouponBuilder(
        private val id: Long?,
        private val userId: Long,
        private val coupon: Coupon,
        private val issuedAt: LocalDateTime,
        private val expiredAt: LocalDateTime,
        private val usedAt: LocalDateTime?,
        private val status: UserCouponStatus,
        private val orderId: Long?
    ) {
        fun build(): UserCoupon {
            return UserCoupon(
                id = id,
                userId = userId,
                coupon = coupon,
                issuedAt = issuedAt,
                expiredAt = expiredAt,
                usedAt = usedAt,
                status = status
            )
        }

        fun withStatus(status: UserCouponStatus): UserCouponBuilder {
            return UserCouponBuilder(
                id = id,
                userId = userId,
                coupon = coupon,
                issuedAt = issuedAt,
                expiredAt = expiredAt,
                usedAt = usedAt,
                status = status,
                orderId = orderId
            )
        }

        fun withUsedAt(usedAt: LocalDateTime?): UserCouponBuilder {
            return UserCouponBuilder(
                id = id,
                userId = userId,
                coupon = coupon,
                issuedAt = issuedAt,
                expiredAt = expiredAt,
                usedAt = usedAt,
                status = status,
                orderId = orderId
            )
        }

        fun asUsed(usedAt: LocalDateTime = LocalDateTime.now()): UserCouponBuilder {
            return UserCouponBuilder(
                id = id,
                userId = userId,
                coupon = coupon,
                issuedAt = issuedAt,
                expiredAt = expiredAt,
                usedAt = usedAt,
                status = UserCouponStatus.USED,
                orderId = orderId
            )
        }

        fun asExpired(): UserCouponBuilder {
            return UserCouponBuilder(
                id = id,
                userId = userId,
                coupon = coupon,
                issuedAt = issuedAt,
                expiredAt = LocalDateTime.now().minusDays(1),
                usedAt = usedAt,
                status = status,
                orderId = orderId
            )
        }
    }

    // 기존 호환성 유지 메서드들 (리팩토링 기간 동안만 유지)
    @Deprecated("새로운 validFixedAmountCoupon() 메서드를 사용하세요", ReplaceWith("validFixedAmountCoupon(id, startAt, endAt)"))
    fun createValidCoupon(
        id: Long? = null,
        startAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1),
        discountPolicy: DiscountPolicy = fixedAmountDiscountPolicy()
    ): Coupon {
        return coupon(
            id = id,
            discountPolicy = discountPolicy,
            startAt = startAt,
            endAt = endAt
        ).build()
    }

    @Deprecated("새로운 inactiveCoupon() 메서드를 사용하세요", ReplaceWith("inactiveCoupon(id, startAt, endAt)"))
    fun createInvalidCoupon(
        id: Long? = null,
        isActive: Boolean = false,
        startAt: LocalDateTime = LocalDateTime.now(),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1)
    ): Coupon {
        return coupon(
            id = id,
            isActive = isActive,
            startAt = startAt,
            endAt = endAt
        ).build()
    }

    @Deprecated("새로운 fixedAmountDiscountPolicy() 메서드를 사용하세요", ReplaceWith("fixedAmountDiscountPolicy()"))
    fun createFixedAmountDiscountPolicy(): DiscountPolicy {
        return fixedAmountDiscountPolicy()
    }

    @Deprecated("새로운 discountContext() 빌더를 사용하세요", ReplaceWith("discountContext().build()"))
    fun createDiscountContext(
        subTotal1: Money = Money.of(10000),
        subTotal2: Money = Money.of(10000),
        timestamp: LocalDateTime = LocalDateTime.now()
    ): DiscountContext.Root {
        return DiscountContext.Root(
            items = listOf(
                standardDiscountContextItem(subTotal = subTotal1, totalAmount = subTotal1 + subTotal2),
                standardDiscountContextItem(
                    orderItemId = 2L,
                    variantId = 2L,
                    subTotal = subTotal2,
                    totalAmount = subTotal1 + subTotal2
                ),
            ),
            timestamp = timestamp,
        )
    }

    @Deprecated("새로운 standardDiscountContextItem() 메서드를 사용하세요", ReplaceWith("standardDiscountContextItem(orderItemId, productId, variantId, quantity, subTotal, totalAmount)"))
    fun createDiscountContextItem(
        orderItemId: Long = 1L,
        productId: Long = 1L,
        variantId: Long = 1L,
        quantity: Int = 1,
        subTotal: Money = Money.of(10000),
        totalAmount: Money = Money.of(20000),
    ): DiscountContext.Item {
        return standardDiscountContextItem(
            orderItemId = orderItemId,
            productId = productId,
            variantId = variantId,
            quantity = quantity,
            subTotal = subTotal,
            totalAmount = totalAmount
        )
    }

    @Deprecated("새로운 userCoupon() 빌더를 사용하세요", ReplaceWith("userCoupon(id, userId, coupon).build()"))
    fun createUserCoupon(
        id: Long? = null,
        userId: Long = 1L,
        coupon: Coupon
    ): UserCoupon {
        return userCoupon(
            id = id,
            userId = userId,
            coupon = coupon
        ).build()
    }
}