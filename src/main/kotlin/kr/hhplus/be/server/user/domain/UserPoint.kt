package kr.hhplus.be.server.user.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class UserPoint(
    val id: Long? = null,
    val userId: Long,
    var balance: BigDecimal = BigDecimal.ZERO,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
) {

    fun charge(amount: BigDecimal, now: LocalDateTime) {
        require(amount > BigDecimal.ZERO) { throw InvalidChargeAmountException() }
        this.balance += amount
        this.updatedAt = now
    }

    fun use(amount: BigDecimal, now: LocalDateTime) {
        require(amount <= balance) { throw InsufficientPointException() }
        this.balance -= amount
        this.updatedAt = now
    }
}