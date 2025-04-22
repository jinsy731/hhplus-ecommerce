package kr.hhplus.be.server.user

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.user.domain.UserPoint
import java.time.LocalDateTime

object UserPointTestFixture {
    fun createUserPoint(
        userId: Long = 1L,
        balance: Money = Money.of(1000),
        updatedAt: LocalDateTime = LocalDateTime.now()
        ) = UserPoint(
        userId = userId,
        balance = balance,
        createdAt = LocalDateTime.now(),
        updatedAt = updatedAt
    )
}