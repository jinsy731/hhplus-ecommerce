package kr.hhplus.be.server.payment.application

import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.payment.domain.model.PaymentContext
import java.time.LocalDateTime
import kotlin.collections.map

class PaymentCommand {
    class Prepare {
        data class Root(
            val order: OrderInfo,
            val timestamp: LocalDateTime,
        )

        data class OrderInfo(
            val id: Long,
            val userId: Long,
            val items: List<OrderItemInfo>,
            val originalTotal: Money,
            val discountedAmount: Money
        )

        data class OrderItemInfo(
            val id: Long,
            val productId: Long,
            val variantId: Long,
            val subTotal: Money,
            val discountedAmount: Money,
        )
    }


    data class Complete(
        val paymentId: Long
    )
}

fun PaymentCommand.Prepare.Root.toPreparePaymentContext() = PaymentContext.Prepare.Root(
    order = this.order.toContext(),
    timestamp = this.timestamp,
)

fun PaymentCommand.Prepare.OrderInfo.toContext() = PaymentContext.Prepare.OrderInfo(
    id = this.id,
    userId = this.userId,
    items = this.items.toContext(),
    originalTotal = this.originalTotal,
    discountedAmount = this.discountedAmount
)

fun List<PaymentCommand.Prepare.OrderItemInfo>.toContext() = this.map { item -> PaymentContext.Prepare.OrderItemInfo(
    id = item.id,
    productId = item.productId,
    variantId = item.variantId,
    subTotal = item.subTotal,
    discountedAmount = item.discountedAmount,
)}