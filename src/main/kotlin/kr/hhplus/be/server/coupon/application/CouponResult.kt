package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.common.PageResult
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import org.springframework.data.domain.Page
import java.time.LocalDateTime

class CouponResult {
    data class Use(
        val discountInfo: List<DiscountInfo>
    )

    data class Issue(
        val userCouponId: Long?,
        val status: UserCouponStatus,
        val expiredAt: LocalDateTime
    )

    data class RetrieveList(
        val coupons: List<UserCouponData>,
        val pageResult: PageResult
    )

    data class UserCouponData(
        val id: Long,
        val couponId: Long,
        val couponName: String,
        val description: String,
        val discountPolicyName: String,
        val value: Number?,
        val status: String,
        val expiredAt: LocalDateTime,
    )
}

fun UserCoupon.toUserCouponData() = CouponResult.UserCouponData(
    id = this.id!!,
    couponId = this.coupon.id!!,
    couponName = this.coupon.name,
    description = this.coupon.description,
    discountPolicyName = this.coupon.discountPolicy.name,
    value = this.coupon.discountPolicy.discountType.getDiscountValue(),
    status = this.status.name,
    expiredAt = this.expiredAt
)