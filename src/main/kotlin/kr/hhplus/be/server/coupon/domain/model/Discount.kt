package kr.hhplus.be.server.coupon.domain.model

import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

interface Discount {
    fun calculateDiscount(now: LocalDateTime, order: Order, targetItems: List<OrderItem>): Map<OrderItem, BigDecimal>
    fun isApplicableTo(context: DiscountContext): Boolean
    fun applicableItems(order: Order, userId: Long): List<OrderItem>
}