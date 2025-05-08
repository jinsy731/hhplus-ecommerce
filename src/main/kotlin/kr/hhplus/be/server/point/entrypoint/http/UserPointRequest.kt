package kr.hhplus.be.server.point.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.point.application.UserPointCommand
import kr.hhplus.be.server.point.entrypoint.http.UserPointRequest.Charge
import java.math.BigDecimal

class UserPointRequest {
    @Schema(description = "잔액 충전 요청")
    data class Charge(
        @Schema(description = "충전할 금액", example = "5000")
        val amount: BigDecimal
    )
}

fun Charge.toCmd(userId: Long) = UserPointCommand.Charge(userId, Money.of(amount))
