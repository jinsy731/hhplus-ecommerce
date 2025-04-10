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
import kr.hhplus.be.server.coupon.domain.InvalidCouponStatus
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
    var status: UserCouponStatus,
) {
    /**
     * 쿠폰 사용 처리
     */
    fun use(time: LocalDateTime) {
        check(status == UserCouponStatus.UNUSED && coupon.isValid()) { throw InvalidCouponStatus() }

        this.usedAt = time
        this.status = UserCouponStatus.USED
    }

    /**
     * 쿠폰이 사용 가능한지 확인
     */
    fun isAvailable(): Boolean {
        return this.status == UserCouponStatus.UNUSED && coupon.isValid()
    }
}

enum class UserCouponStatus {
    UNUSED, USED, EXPIRED
}