package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.order.domain.model.OrderContext
import java.time.LocalDateTime

class OrderCommand {
    class Create {
        data class Root(
            val userId: Long,
            val products: List<ProductInfo.CreateOrder.Root>,
            val orderItems: List<OrderItem>,
            val timestamp: LocalDateTime
        )
        data class OrderItem(
            val productId: Long,
            val variantId: Long,
            val quantity: Int
        )
    }

    data class ApplyDiscount(val orderId: Long, val discountInfos: List<DiscountInfo>)

}

fun OrderCommand.Create.Root.toOrderCreateContext(): OrderContext.Create.Root {
    return OrderContext.Create.Root(
        userId = this.userId,
        timestamp = this.timestamp,
        items = this.orderItems.map { item ->
            val product = this.products.find { product -> product.productId == item.productId } ?: throw IllegalStateException()
            val variant = product.variants.find { variant -> variant.variantId == item.variantId } ?: throw IllegalStateException()
            OrderContext.Create.Item(
                productId = product.productId,
                variantId = item.variantId,
                quantity = item.quantity,
                unitPrice = variant.unitPrice
            )
        }
    )
}