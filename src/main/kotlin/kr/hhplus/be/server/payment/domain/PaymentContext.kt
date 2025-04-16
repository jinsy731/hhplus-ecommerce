package kr.hhplus.be.server.payment.domain

import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentContext {
    class Prepare {
        data class Root(
            val order: OrderInfo,
            val timestamp: LocalDateTime,
            val payMethods: List<PayMethod>
        )

        data class PayMethod(
            val type: PaymentMethodType,
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
            val subTotal: BigDecimal,
            val discountedAmount: BigDecimal
        )
    }


    data class Complete(
        val paymentId: Long
    )


}