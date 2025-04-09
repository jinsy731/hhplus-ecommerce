package kr.hhplus.be.server.user.domain

import kr.hhplus.be.server.user.InvalidChargeAmountException

data class UserPoint(
    val id: Long? = null,
    val userId: Long,
    val balance: Long = 0
) {
    fun charge(amount: Long): UserPoint {
        require(amount > 0) { throw InvalidChargeAmountException() }
        return this.copy(balance = this.balance + amount)
    }
}