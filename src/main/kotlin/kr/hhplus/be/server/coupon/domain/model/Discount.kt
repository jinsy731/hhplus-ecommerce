package kr.hhplus.be.server.coupon.domain.model

import kr.hhplus.be.server.shared.domain.Money

interface Discount {
    fun calculateDiscount(context: DiscountContext.Root, targetItems: List<Long>): Map<DiscountContext.Item, Money>
    fun isApplicableTo(context: DiscountContext.Item): Boolean
    fun getApplicableItems(context: DiscountContext.Root): List<Long>
}