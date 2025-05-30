package kr.hhplus.be.server.coupon.application.mapper

import kr.hhplus.be.server.coupon.application.dto.CouponResult
import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.stereotype.Component

@Component
class CouponMapper {

    fun mapToDiscountInfoList(discountLines: List<DiscountLine>): List<DiscountInfo> {
        return discountLines.map { 
            DiscountInfo(
                orderItemId = it.orderItemId,
                amount = it.amount,
                sourceId = it.sourceId,
                sourceType = it.type.toString()
            )
        }
    }

    fun mapToUserCouponData(userCoupon: UserCoupon, coupon: Coupon): CouponResult.UserCouponData {
        return CouponResult.UserCouponData(
            id = userCoupon.id!!,
            couponId = userCoupon.couponId,
            couponName = coupon.name,
            description = coupon.description,
            discountPolicyName = coupon.discountPolicy.name,
            value = coupon.discountPolicy.discountType.getDiscountValue(),
            status = userCoupon.status.name,
            expiredAt = userCoupon.expiredAt
        )
    }

    // OrderMapper로 이동됨
} 