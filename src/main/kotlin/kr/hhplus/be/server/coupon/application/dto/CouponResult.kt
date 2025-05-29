package kr.hhplus.be.server.coupon.application.dto

import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.shared.web.PageResult
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

    data class AsyncIssue(
        val couponId: Long,
        val status: String
    )

    data class AsyncIssueStatus(
        val couponId: Long,
        val status: String,
        val userCouponId: Long? = null
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

// 매핑 함수는 CouponMapper로 이동됨