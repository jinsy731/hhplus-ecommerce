package kr.hhplus.be.server.payment.application

import kr.hhplus.be.server.payment.domain.PaymentContext
import kr.hhplus.be.server.payment.domain.PaymentMethodType
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentCommand {
    class Prepare {
        data class Root(
            val order: OrderInfo,
            val timestamp: LocalDateTime,
            val payMethods: List<PayMethod>
        )

        data class PayMethod(
            val type: String,
            val amount: BigDecimal
        )

        data class OrderInfo(
            val id: Long,
            val userId: Long,
            val items: List<OrderItemInfo>,
            val originalTotal: BigDecimal,
            val discountedAmount: BigDecimal
        )

        data class OrderItemInfo(
            val id: Long,
            val productId: Long,
            val variantId: Long,
            val quantity: Int,
            val unitPrice: BigDecimal
        )
    }


    data class Complete(
        val paymentId: Long
    )
}

fun PaymentCommand.Prepare.Root.toPreparePaymentContext() = PaymentContext.Prepare.Root(
    order = this.order.toContext(),
    timestamp = this.timestamp,
    payMethods = this.payMethods.map { PaymentContext.Prepare.PayMethod(
        type = PaymentMethodType.valueOf(it.type),
        amount = it.amount
    ) }
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
    quantity = item.quantity,
    unitPrice = item.unitPrice
)}