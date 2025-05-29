package kr.hhplus.be.server.coupon.application.mapper

import kr.hhplus.be.server.coupon.application.dto.CouponResult
import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
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

    fun mapToUserCouponData(userCoupon: UserCoupon): CouponResult.UserCouponData {
        return CouponResult.UserCouponData(
            id = userCoupon.id!!,
            couponId = userCoupon.coupon.id!!,
            couponName = userCoupon.coupon.name,
            description = userCoupon.coupon.description,
            discountPolicyName = userCoupon.coupon.discountPolicy.name,
            value = userCoupon.coupon.discountPolicy.discountType.getDiscountValue(),
            status = userCoupon.status.name,
            expiredAt = userCoupon.expiredAt
        )
    }

    // OrderMapper로 이동됨
} 