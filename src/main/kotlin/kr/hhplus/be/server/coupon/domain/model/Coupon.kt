package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.CouponTargetNotFoundException
import kr.hhplus.be.server.common.exception.ExceededMaxCouponLimitException
import kr.hhplus.be.server.common.exception.InvalidCouponStatusException
import java.time.LocalDateTime

/**
 * 쿠폰 엔티티
 */
@Entity
@Table(name = "coupons")
class Coupon(
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val description: String,
    
    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "discount_policy_id", nullable = false)
    val discountPolicy: DiscountPolicy,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    var maxIssueLimit: Int,

    @Column(nullable = false)
    var issuedCount: Int = 0,

    @Column(nullable = false)
    val startAt: LocalDateTime,

    @Column(nullable = false)
    val endAt: LocalDateTime,

    @Column(nullable = false)
    val validDays: Int,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
): Discount {
    /**
     * 쿠폰이 유효한지 확인
     */
    fun validatUsability(now: LocalDateTime) {
        if(!(isActive && now.isAfter(startAt) && now.isBefore(endAt)))
            throw InvalidCouponStatusException()
    }
    
    /**
     * 쿠폰 할인 금액 계산
     */
    override fun calculateDiscount(context: DiscountContext.Root, applicableItemIds: List<Long>): Map<DiscountContext.Item, Money> {
        validatUsability(context.timestamp)
        val applicableItems = context.items.filter { applicableItemIds.contains(it.orderItemId) }
        return discountPolicy.calculateDiscount(context.copy(items = applicableItems))
    }

    override fun isApplicableTo(context: DiscountContext.Item): Boolean {
        return this.discountPolicy.discountCondition.isSatisfiedBy(context)
    }

    override fun getApplicableItems(context: DiscountContext.Root): List<Long> {
        val applicableItemIds = context.items
            .filter { isApplicableTo(it) }
            .map { it.orderItemId }

        if(applicableItemIds.isEmpty()) throw CouponTargetNotFoundException()

        return applicableItemIds
    }

    fun issueTo(userId: Long, now: LocalDateTime = LocalDateTime.now()): UserCoupon {
        check(this.issuedCount < this.maxIssueLimit) { throw ExceededMaxCouponLimitException() }
        this.issuedCount++

        return UserCoupon(
            userId = userId,
            coupon = this,
            issuedAt = now,
            expiredAt = now.plusDays(this.validDays.toLong()),
        )
    }
}
