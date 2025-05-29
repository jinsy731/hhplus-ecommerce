package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.product.application.dto.toCreateOrderProductInfo
import kr.hhplus.be.server.product.domain.product.model.Product
import java.time.LocalDateTime

class OrderCriteria {
    // 주문서 생성
    class CreateOrderSheet {
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

    // 결제 처리
    class ProcessPayment {
        data class Root(
            val orderId: Long,
            val pgPaymentId: String,
            val paymentMethod: String,
            val timestamp: LocalDateTime = LocalDateTime.now()
        )
    }

    // 기존 주문 생성 (호환성 유지)
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

// 주문서 생성 Command 변환
fun OrderCriteria.CreateOrderSheet.Root.toCreateOrderSheetCommand(products: List<Product>): OrderCommand.CreateOrderSheet.Root {
    return OrderCommand.CreateOrderSheet.Root(
        userId = this.userId,
        products = products.toCreateOrderProductInfo(),
        orderItems = this.items.toCreateOrderSheetCommand(),
        userCouponIds = this.userCouponIds,
        timestamp = this.timestamp
    )
}

fun List<OrderCriteria.CreateOrderSheet.Item>.toCreateOrderSheetCommand(): List<OrderCommand.CreateOrderSheet.OrderItem> {
    return this.map { item -> OrderCommand.CreateOrderSheet.OrderItem(
        productId = item.productId,
        variantId = item.variantId,
        quantity = item.quantity
    ) }
}

// 결제 처리 Command 변환
fun OrderCriteria.ProcessPayment.Root.toProcessPaymentCommand(): OrderCommand.ProcessPayment.Root {
    return OrderCommand.ProcessPayment.Root(
        orderId = this.orderId,
        pgPaymentId = this.pgPaymentId,
        paymentMethod = this.paymentMethod,
        timestamp = this.timestamp
    )
}

// 기존 주문 생성 Command 변환 (호환성 유지)
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

//
//// Coupon Command
//fun OrderCriteria.PlaceOrder.Root.toUseCouponCommand(order: Order): CouponCommand.Use.Root {
//    return CouponCommand.Use.Root(
//        userId = this.userId,
//        userCouponIds = this.userCouponIds,
//        items = order.orderItems.toUseCouponCommandItem(),
//        timestamp = this.timestamp,
//        totalAmount = order.originalTotal
//    )
//}
//
//// Payment Command
//fun OrderCriteria.PlaceOrder.Root.toPreparePaymentCommand(order: Order): PaymentCommand.Prepare.Root {
//    return PaymentCommand.Prepare.Root(
//        order = PaymentCommand.Prepare.OrderInfo(
//            id = order.id,
//            userId = this.userId,
//            items = order.orderItems.toPreparePaymentCommandItem(),
//            originalTotal = order.originalTotal,
//            discountedAmount = order.discountedAmount
//        ),
//        timestamp = this.timestamp,
//    )
//}