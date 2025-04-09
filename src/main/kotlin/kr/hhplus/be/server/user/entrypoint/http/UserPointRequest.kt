package kr.hhplus.be.server.user.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.user.application.UserPointCommand
import kr.hhplus.be.server.user.entrypoint.http.UserPointRequest.Charge

class UserPointRequest {
    @Schema(description = "잔액 충전 요청")
    data class Charge(
        @Schema(description = "충전할 금액", example = "5000")
        val amount: Long
    )
}

fun Charge.toCmd(userId: Long) = UserPointCommand.Charge(userId, amount)
