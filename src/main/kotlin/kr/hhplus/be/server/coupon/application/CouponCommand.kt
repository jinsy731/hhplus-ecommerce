package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.order.domain.Order
import java.time.LocalDateTime

class CouponCommand {
    data class ApplyToOrder(
        val userId: Long,
        val order: Order,
        val userCouponIds: List<Long>,
        val now: LocalDateTime
    )

    data class Issue(
        val userId: Long,
        val couponId: Long
    )
}