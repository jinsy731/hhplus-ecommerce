package kr.hhplus.be.server.payment.application

import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.payment.domain.PaymentMethodType
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentCommand {
    data class Prepare(
        val order: Order,
        val now: LocalDateTime,
        val payMethods: List<PayMethod>
    )

    data class Complete(
        val paymentId: Long
    )

    data class PayMethod(
        val type: PaymentMethodType,
        val amount: BigDecimal
    )
}