package kr.hhplus.be.server.user

data class ChargeBalanceRequest(
    val amount: Int
)

data class BalanceResponse(
    val userId: Long,
    val balance: Int
)
