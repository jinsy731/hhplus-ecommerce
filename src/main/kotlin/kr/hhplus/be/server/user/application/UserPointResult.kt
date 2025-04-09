package kr.hhplus.be.server.user.application

import java.time.LocalDateTime

class UserPointResult {
    data class Charge(
        val pointAfterCharge: Long,
        val updatedAt: LocalDateTime?
    )
    data class Retrieve(
        val point: Long,
        val updatedAt: LocalDateTime?
    )
}