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

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

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
     * 쿠폰 사용 처리 (쿠폰 유효성 검증은 서비스 레이어에서 수행)
     */
    fun use(now: LocalDateTime, orderId: Long) {
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

    /**
     * 도메인 서비스를 통한 할인 계산 및 사용 처리
     * 도메인 서비스에 위임하여 복잡한 비즈니스 로직을 처리
     */
    fun calculateDiscountAndUse(
        domainService: kr.hhplus.be.server.coupon.domain.service.CouponDomainService,
        context: DiscountContext.Root, 
        orderId: Long
    ): List<DiscountLine> {
        return domainService.calculateDiscountAndUse(this, context, orderId)
    }
}

enum class UserCouponStatus {
    UNUSED, USED, EXPIRED
}