package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.coupon.application.DiscountInfo
import java.math.BigDecimal

class OrderItems(
    private val items: MutableList<OrderItem> = mutableListOf()
) : Iterable<OrderItem> {

    fun size() = this.items.size

    fun add(orderItem: OrderItem, order: Order) {
        items.add(orderItem)
        orderItem.order = order
    }

    fun asList(): List<OrderItem> = items.toList()


    fun calculateOriginalTotal(): Money = items.fold(Money.ZERO) { acc, it -> acc + it.subTotal }

    fun totalDiscount(): BigDecimal = items.sumOf { it.discountAmount.amount }

    fun applyDiscounts(discountInfos: List<DiscountInfo>) {
        discountInfos.forEach { discountLine ->
            val target = items.find { it.id == discountLine.orderItemId }
                ?: throw IllegalStateException("OrderItem not found for discount: ${discountLine.orderItemId}")
            target.applyDiscount(discountLine.amount)
        }
    }

    override fun iterator(): Iterator<OrderItem> = items.iterator()
}