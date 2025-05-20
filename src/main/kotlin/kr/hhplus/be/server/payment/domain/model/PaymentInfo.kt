package kr.hhplus.be.server.payment.domain.model

import java.math.BigDecimal

class PaymentInfo {
    class Prepare {
        data class OrderInfo(
            val id: Long,
            val originalAmount: BigDecimal,
            val discountedAmount: BigDecimal,
            val items: List<OrderItemInfo>
        )

        data class OrderItemInfo(
            val id: Long,
            val originalAmount: BigDecimal,
            val discountedAmount: BigDecimal
        )
    }
}

