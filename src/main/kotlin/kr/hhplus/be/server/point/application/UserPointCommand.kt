package kr.hhplus.be.server.point.application

import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

class UserPointCommand {
    data class Charge(
        val userId: Long,
        val amount: Money,
        val now: LocalDateTime = LocalDateTime.now()
    )

    data class Use(
        val userId: Long,
        val amount: Money,
        val now: LocalDateTime = LocalDateTime.now(),
        val context: OrderSagaContext
    )

    data class Restore(
        val userId: Long,
        val amount: Money,
        val now: LocalDateTime = LocalDateTime.now(),
        val context: OrderSagaContext
    )

    data class Retrieve(val userId: Long)
}