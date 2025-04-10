package kr.hhplus.be.server.coupon.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jdk.jpackage.internal.Arguments.CLIOptions.context
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isActive

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
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discount_policy_id", nullable = false)
    val discountPolicy: DiscountPolicy,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val maxIssueLimit: Int,

    @Column(nullable = false)
    val issuedCount: Int = 0,

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
) {
    /**
     * 쿠폰이 유효한지 확인
     */
    fun isValid(now: LocalDateTime): Boolean {
        return isActive && now.isAfter(startAt) && now.isBefore(endAt)
    }
    
    /**
     * 쿠폰 할인 금액 계산
     */
    fun calculateDiscount(now: LocalDateTime, price: BigDecimal): BigDecimal {
        return if (isValid(now)) {
            discountPolicy.calculateDiscount(price)
        } else {
            BigDecimal.ZERO
        }
    }

    fun isApplicableTo(context: DiscountContext): Boolean {
        return this.discountPolicy.discountCondition.isSatisfiedBy(context)
    }
}
