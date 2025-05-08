package kr.hhplus.be.server.point.application

import kr.hhplus.be.server.shared.domain.Money
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