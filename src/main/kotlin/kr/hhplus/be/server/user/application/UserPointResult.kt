package kr.hhplus.be.server.user.application

import java.time.LocalDateTime

class UserPointResult {
    data class Charge(
        val userId: Long,
        val pointAfterCharge: Long,
        val updatedAt: LocalDateTime?
    )
    data class Retrieve(
        val userId: Long,
        val point: Long,
        val updatedAt: LocalDateTime?
    )
}