package kr.hhplus.be.server.common.domain

import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.require

@Embeddable
data class Money(val amount: BigDecimal) : Comparable<Money> {

    private constructor() : this(BigDecimal.ZERO)

    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다.(amount = $amount)"}
        require(amount.scale() <= MONEY_SCALE) {
            "소수점은 $MONEY_SCALE 자리까지만 허용됩니다. (현재: ${amount.scale()})"
        }
    }

    operator fun plus(other: Money): Money =
        Money(this.amount.add(other.amount).setScale(MONEY_SCALE, MONEY_ROUNDING))

    operator fun minus(other: Money): Money =
        Money(this.amount.subtract(other.amount).setScale(MONEY_SCALE, MONEY_ROUNDING))

    operator fun times(multiplier: BigDecimal): Money =
        Money(this.amount.multiply(multiplier).setScale(MONEY_SCALE, MONEY_ROUNDING))

    operator fun div(divisor: BigDecimal): Money =
        Money(this.amount.divide(divisor, MONEY_SCALE, MONEY_ROUNDING))

    override operator fun compareTo(other: Money): Int =
        this.amount.compareTo(other.amount)

    fun isPositive(): Boolean = amount > BigDecimal.ZERO
    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    companion object {
        private const val MONEY_SCALE = 2
        private val MONEY_ROUNDING = RoundingMode.HALF_UP

        val ZERO = Money(BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING))

        fun of(value: Long): Money = Money(BigDecimal.valueOf(value).setScale(MONEY_SCALE, MONEY_ROUNDING))
        fun of(value: BigDecimal): Money = Money(value.setScale(MONEY_SCALE, MONEY_ROUNDING))
    }
}