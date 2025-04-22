package kr.hhplus.be.server.user.application

import kr.hhplus.be.server.common.domain.Money
import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointResult {
    data class Charge(
        val userId: Long,
        val pointAfterCharge: Money,
        val updatedAt: LocalDateTime?
    )
    data class Retrieve(
        val userId: Long,
        val point: Money,
        val updatedAt: LocalDateTime?
    )
}