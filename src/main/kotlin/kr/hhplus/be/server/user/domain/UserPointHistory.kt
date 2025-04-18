package kr.hhplus.be.server.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "USER_POINT_HISTORY",
    indexes = [Index(name = "idx_user_point_history_user_id", columnList = "user_id")]
)class UserPointHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false)
    val userId: Long,
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,
    @Column(nullable = false)
    val amount: BigDecimal,
    @Column(nullable = false)
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
