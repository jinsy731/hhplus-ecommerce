package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import java.math.BigDecimal

class CouponResult {
    data class ApplyToOrder(
        val discountLine: List<DiscountLine>
    )
}