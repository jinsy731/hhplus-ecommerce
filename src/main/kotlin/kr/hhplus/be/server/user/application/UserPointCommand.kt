package kr.hhplus.be.server.user.application

import java.time.LocalDateTime

class UserPointCommand {
    data class Charge(
        val userId: Long,
        val amount: Long,
        val time: LocalDateTime = LocalDateTime.now()
    )

    data class Retrieve(val userId: Long)
}