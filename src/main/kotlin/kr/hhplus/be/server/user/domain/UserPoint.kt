package kr.hhplus.be.server.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.common.exception.InsufficientPointException
import kr.hhplus.be.server.common.exception.InvalidChargeAmountException
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "USER_POINT")
class UserPoint(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false)
    val userId: Long,
    @Column(nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false)
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    @Column(nullable = false)
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