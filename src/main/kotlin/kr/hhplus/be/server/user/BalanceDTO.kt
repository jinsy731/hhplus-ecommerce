package kr.hhplus.be.server.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "잔액 충전 요청")
data class ChargeBalanceRequest(
    @Schema(description = "충전할 금액", example = "5000")
    val amount: Int
)


@Schema(description = "잔액 응답")
data class BalanceResponse(
    @Schema(description = "유저 ID", example = "1")
    val userId: Long,
    @Schema(description = "잔액", example = "12000")
    val balance: Int
)
