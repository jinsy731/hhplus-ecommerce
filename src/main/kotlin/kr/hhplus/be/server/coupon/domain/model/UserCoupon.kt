package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.exception.ExpiredCouponException
import kr.hhplus.be.server.shared.exception.InvalidCouponStatusException
import java.time.LocalDateTime

/**
 * 사용자 쿠폰 엔티티 (발급된 쿠폰)
 */
@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_coupon", columnNames = ["userId", "coupon_id"])
    ]
)
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

    @Column(nullable = false) @Enumerated(EnumType.STRING)
    var status: UserCouponStatus = UserCouponStatus.UNUSED,

    @Column(nullable = true)
    var orderId: Long? = null,

    @Version
    var version: Long? = null
) {
    /**
     * 쿠폰 사용 처리
     */
    fun use(now: LocalDateTime, orderId: Long) {
        coupon.validatUsability(now)
        check(status == UserCouponStatus.UNUSED) { throw InvalidCouponStatusException() }
        check(expiredAt.isAfter(now)) {
            this.status = UserCouponStatus.EXPIRED
            throw ExpiredCouponException()
        }

        this.usedAt = now
        this.status = UserCouponStatus.USED
        this.orderId = orderId
    }

    /**
     * 쿠폰 복구 처리
     */
    fun restore() {
        check(status == UserCouponStatus.USED) { throw InvalidCouponStatusException() }
        this.usedAt = null
        this.status = UserCouponStatus.UNUSED
        this.orderId = null
    }

    fun calculateDiscountAndUse(context: DiscountContext.Root, orderId: Long): List<DiscountLine> {
        val applicableItems = coupon.getApplicableItems(context)

        use(context.timestamp, orderId)

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