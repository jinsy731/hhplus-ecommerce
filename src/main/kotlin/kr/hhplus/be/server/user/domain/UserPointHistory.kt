package kr.hhplus.be.server.user.domain

import kr.hhplus.be.server.common.BaseTimeEntity
import java.time.LocalDateTime

data class UserPointHistory(
    val id: Long? = null,
    val userId: Long,
    val transactionType: TransactionType,
    val amount: Long
): BaseTimeEntity() {
    companion object {
        fun createChargeHistory(userId: Long, amount: Long, updatedAt: LocalDateTime): UserPointHistory = UserPointHistory(
            userId = userId,
            transactionType = TransactionType.CHARGE,
            amount = amount
        ).apply { this.updatedAt = updatedAt }
        fun createUseHistory(userId: Long, amount: Long, updatedAt: LocalDateTime): UserPointHistory = UserPointHistory(
            userId = userId,
            transactionType = TransactionType.USE,
            amount = amount
        ).apply { this.updatedAt = updatedAt }
    }
}

enum class TransactionType {
    CHARGE, USE
}
