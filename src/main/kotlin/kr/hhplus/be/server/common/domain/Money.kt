package kr.hhplus.be.server.common.domain

import jakarta.persistence.Embeddable
import java.math.BigDecimal
import kotlin.require

@Embeddable
data class Money(val amount: BigDecimal) {
    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다."}
    }

    operator fun plus(other: Money): Money = Money(this.amount.add(other.amount))
    operator fun minus(other: Money): Money = Money(this.amount.subtract(other.amount))

    companion object {
        val ZERO = Money(BigDecimal.ZERO)
    }
}