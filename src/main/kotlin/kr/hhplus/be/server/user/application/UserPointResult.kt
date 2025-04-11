package kr.hhplus.be.server.user.application

import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointResult {
    data class Charge(
        val userId: Long,
        val pointAfterCharge: BigDecimal,
        val updatedAt: LocalDateTime?
    )
    data class Retrieve(
        val userId: Long,
        val point: BigDecimal,
        val updatedAt: LocalDateTime?
    )
}