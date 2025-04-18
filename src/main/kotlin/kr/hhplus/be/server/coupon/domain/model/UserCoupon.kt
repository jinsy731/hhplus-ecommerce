package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.common.exception.ExpiredCouponException
import kr.hhplus.be.server.common.exception.InvalidCouponStatusException
import java.time.LocalDateTime

/**
 * 사용자 쿠폰 엔티티 (발급된 쿠폰)
 */
@Entity
@Table(name = "user_coupons")
class UserCoupon(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coupon_id", nullable = false)
    val coupon: Coupon,

    @Column(nullable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val expiredAt: LocalDateTime,

    @Column
    var usedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var status: UserCouponStatus = UserCouponStatus.UNUSED,
) {
    /**
     * 쿠폰 사용 처리
     */
    fun use(now: LocalDateTime) {
        coupon.validate(now)
        check(status == UserCouponStatus.UNUSED) { throw InvalidCouponStatusException() }
        check(expiredAt.isAfter(now)) {
            this.status = UserCouponStatus.EXPIRED
            throw ExpiredCouponException()
        }

        this.usedAt = now
        this.status = UserCouponStatus.USED
    }

    fun calculateDiscountAndUse(context: DiscountContext.Root): List<DiscountLine> {
        val applicableItems = coupon.getApplicableItems(context)

        use(context.timestamp)

        val orderItemsDiscountMap = coupon.calculateDiscount(context, applicableItems)
        return DiscountLine.from(
            sourceId = this.coupon.id!!,
            discountMethod = DiscountMethod.COUPON,
            orderItemsDiscountMap = orderItemsDiscountMap,
            now = context.timestamp)
    }
}

enum class UserCouponStatus {
    UNUSED, USED, EXPIRED
}