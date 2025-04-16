package kr.hhplus.be.server.user.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class UserPointHistory(
    val id: Long? = null,
    val userId: Long,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val createdAt: LocalDateTime? = null
) {
    companion object {
        fun createChargeHistory(userId: Long, amount: BigDecimal, now: LocalDateTime): UserPointHistory = UserPointHistory(
            userId = userId,
            transactionType = TransactionType.CHARGE,
            amount = amount,
            createdAt = now
        )
        fun createUseHistory(userId: Long, amount: BigDecimal, now: LocalDateTime): UserPointHistory = UserPointHistory(
            userId = userId,
            transactionType = TransactionType.USE,
            amount = amount,
            createdAt = now
        )
    }
}

enum class TransactionType {
    CHARGE, USE
}
