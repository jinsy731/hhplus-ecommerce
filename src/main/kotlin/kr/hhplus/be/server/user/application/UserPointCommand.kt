package kr.hhplus.be.server.user.application

import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointCommand {
    data class Charge(
        val userId: Long,
        val amount: BigDecimal,
        val now: LocalDateTime = LocalDateTime.now()
    )

    data class Use(
        val userId: Long,
        val amount: BigDecimal,
        val now: LocalDateTime = LocalDateTime.now()
    )

    data class Retrieve(val userId: Long)
}