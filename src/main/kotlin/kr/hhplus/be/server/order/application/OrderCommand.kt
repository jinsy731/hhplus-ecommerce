package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.product.domain.Product
import java.time.LocalDateTime

class OrderCommand {
    data class Create(
        val userId: Long,
        val products: List<Product>,
        val orderItems: List<OrderItemCommand.Create>,
        val now: LocalDateTime
    )

    data class ApplyDiscount(
        val order: Order,
        val discountLines: List<DiscountLine>
    )
}

class OrderItemCommand {
    data class Create(
        val productId: Long,
        val variantId: Long,
        val quantity: Int
    )
}