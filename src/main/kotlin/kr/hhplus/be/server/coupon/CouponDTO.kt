package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.common.PageInfo
import java.time.LocalDate

data class IssueCouponRequest(val couponId: Long)

data class IssueCouponResponse(val userCouponId: Long, val status: String)

data class UserCouponResponse(
    val userCouponId: Long,
    val couponName: String,
    val discountType: String,
    val value: String,
    val status: String,
    val expiredAt: LocalDate
)

data class UserCouponListResponse(
    val coupons: List<UserCouponResponse>,
    val pageInfo: PageInfo
)
