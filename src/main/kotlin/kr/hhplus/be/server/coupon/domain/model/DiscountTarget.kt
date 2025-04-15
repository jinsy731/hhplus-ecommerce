package kr.hhplus.be.server.coupon.domain.model

import java.math.BigDecimal

data class DiscountTarget(
    val orderItemId: Long,
    val discountAmount: BigDecimal
)