package kr.hhplus.be.server.coupon.domain.model

import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

interface Discount {
    fun calculateDiscount(context: DiscountContext.Root, targetItems: List<Long>): Map<DiscountContext.Item, BigDecimal>
    fun isApplicableTo(context: DiscountContext.Item): Boolean
    fun getApplicableItems(context: DiscountContext.Root): List<Long>
}