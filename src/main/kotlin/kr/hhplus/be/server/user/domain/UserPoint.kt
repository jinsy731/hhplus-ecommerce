package kr.hhplus.be.server.user.domain

import kr.hhplus.be.server.common.BaseTimeEntity
import kr.hhplus.be.server.user.InvalidChargeAmountException
import java.time.LocalDateTime

data class UserPoint(
    val id: Long? = null,
    val userId: Long,
    val balance: Long = 0
): BaseTimeEntity() {
    fun charge(amount: Long, updatedAt: LocalDateTime): UserPoint {
        require(amount > 0) { throw InvalidChargeAmountException() }
        return this.copy(balance = this.balance + amount)
            .apply { this.updatedAt = updatedAt }
    }
}