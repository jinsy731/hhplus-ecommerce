package kr.hhplus.be.server.point.application

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
        val now: LocalDateTime = LocalDateTime.now()
    )

    data class Retrieve(val userId: Long)
}