package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.order.domain.model.Order
import java.time.LocalDateTime

class CouponCommand {
    data class ApplyToOrder(
        val userId: Long,
        val order: Order,
        val userCouponIds: List<Long>,
        val now: LocalDateTime
    )
}