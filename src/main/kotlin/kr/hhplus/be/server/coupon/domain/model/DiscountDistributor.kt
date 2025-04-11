package kr.hhplus.be.server.coupon.domain.model

import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.collections.lastIndex
import kotlin.collections.mapIndexed

object DiscountDistributor {
    fun distribute(
        items: List<OrderItem>,
        sourceId: Long,
        now: LocalDateTime,
        totalDiscount: BigDecimal
    ): List<DiscountLine> {
        val total = items.sumOf { it.subTotal() }
        var remaining = totalDiscount
        return items.mapIndexed { index, item ->
            val proportion = item.subTotal().divide(total)
            val itemDiscount = if (index == items.lastIndex)
                remaining
            else
                (totalDiscount * proportion).also { remaining -= it }

            DiscountLine(
                orderItemId = item.id,
                type = DiscountMethod.COUPON,
                sourceId = sourceId,
                amount = itemDiscount,
                createdAt = now
            )
        }
    }
}
