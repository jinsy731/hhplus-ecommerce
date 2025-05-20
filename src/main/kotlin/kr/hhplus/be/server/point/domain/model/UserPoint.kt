package kr.hhplus.be.server.point.domain.model

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

@Entity
@Table(
    name = "USER_POINT",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])],
    indexes = [Index(name = "idx_user_id", columnList = "user_id")]
)class UserPoint(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val userId: Long,
    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "balance"))
    var balance: Money = Money.ZERO,
    @Column(nullable = false)
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    @Version
    var version: Long? = null
) {

    fun charge(amount: Money, now: LocalDateTime) {
        this.balance += amount
        this.updatedAt = now
    }

    fun use(amount: Money, now: LocalDateTime) {
        this.balance -= amount
        this.updatedAt = now
    }
}