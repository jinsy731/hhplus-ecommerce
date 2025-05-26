package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.domain.model.Order
import java.time.LocalDateTime

data class OrderSagaContext(
    val order: Order,
    val timestamp: LocalDateTime,
    val discountInfos: List<DiscountInfo> = emptyList(),
    val userCouponIds: List<Long> = emptyList(),
    val paymentId: Long? = null,
    val failedReason: String? = null
)

