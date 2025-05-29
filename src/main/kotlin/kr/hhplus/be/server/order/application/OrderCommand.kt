package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.model.OrderContext
import kr.hhplus.be.server.product.application.dto.ProductInfo
import java.time.LocalDateTime

class OrderCommand {
    // 주문서 생성
    class CreateOrderSheet {
        data class Root(
            val userId: Long,
            val products: List<ProductInfo.CreateOrder.Root>,
            val orderItems: List<OrderItem>,
            val userCouponIds: List<Long>,
            val timestamp: LocalDateTime
        )
        data class OrderItem(
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
            val timestamp: LocalDateTime
        )
    }

    // 기존 주문 생성 (호환성 유지)
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



}

// 주문서 생성 Context 변환
fun OrderCommand.CreateOrderSheet.Root.toOrderCreateContext(): OrderContext.Create.Root {
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

// 기존 주문 생성 Context 변환 (호환성 유지)
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