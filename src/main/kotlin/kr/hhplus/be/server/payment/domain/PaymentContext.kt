package kr.hhplus.be.server.payment.domain

import kr.hhplus.be.server.common.domain.Money
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentContext {
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
            val discountedAmount: Money
        )
    }


    data class Complete(
        val paymentId: Long
    )
}