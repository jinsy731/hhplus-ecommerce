package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.coupon.application.CouponCommand
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.toPreparePaymentCommandItem
import kr.hhplus.be.server.order.application.toUseCouponCommandItem
import kr.hhplus.be.server.product.application.toCreateOrderProductInfo
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.product.domain.product.Product
import java.time.LocalDateTime

class OrderCriteria {
    class PlaceOrder {
        data class Root(
            val userId: Long,
            val items: List<Item>,
            val userCouponIds: List<Long>,
            val timestamp: LocalDateTime = LocalDateTime.now()
        )

        data class Item(
            val productId: Long,
            val variantId: Long,
            val quantity: Int
        )
    }
}

// Order Command
fun OrderCriteria.PlaceOrder.Root.toCreateOrderCommand(products: List<Product>): OrderCommand.Create.Root {
    return OrderCommand.Create.Root(
        userId = this.userId,
        products = products.toCreateOrderProductInfo(),
        orderItems = this.items.toCreateOrderCommand(),
        timestamp = this.timestamp
    )
}

fun List<OrderCriteria.PlaceOrder.Item>.toCreateOrderCommand(): List<OrderCommand.Create.OrderItem> {
    return this.map { item -> OrderCommand.Create.OrderItem(
        productId = item.productId,
        variantId = item.variantId,
        quantity = item.quantity
    ) }
}


// Coupon Command
fun OrderCriteria.PlaceOrder.Root.toUseCouponCommand(order: Order): CouponCommand.Use.Root {
    return CouponCommand.Use.Root(
        userId = this.userId,
        userCouponIds = this.userCouponIds,
        items = order.orderItems.toUseCouponCommandItem(),
        timestamp = this.timestamp,
        totalAmount = order.originalTotal
    )
}

// Payment Command
fun OrderCriteria.PlaceOrder.Root.toPreparePaymentCommand(order: Order): PaymentCommand.Prepare.Root {
    return PaymentCommand.Prepare.Root(
        order = PaymentCommand.Prepare.OrderInfo(
            id = order.id,
            userId = this.userId,
            items = order.orderItems.toPreparePaymentCommandItem(),
            originalTotal = order.originalTotal,
            discountedAmount = order.discountedAmount
        ),
        timestamp = this.timestamp,
    )
}