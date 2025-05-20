package kr.hhplus.be.server.point.domain

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointTest {
    @Test
    fun `✅ 유저 포인트 충전_충전 금액이 잔액에 반영되어야 한다`() {
        // arrange
        val userPoint = UserPoint(1L, 1L, Money.ZERO)
        val time = LocalDateTime.now()
        // act
        userPoint.charge(Money.of(100), time)
        // assert
        userPoint.balance shouldBe Money.of(100)
        userPoint.updatedAt shouldBe time
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1"])
    fun `⛔️유저 포인트 충전 실패_충전 금액이 0보다 작거나 같으면 IllegalArgumentException 이 발생해야한다`(amount: BigDecimal) {
        val userPoint = UserPoint(1L, 1L, Money.ZERO)
        val ex = shouldThrowExactly<IllegalArgumentException> { userPoint.charge(Money.of(amount), LocalDateTime.now()) }
    }
    
    @Test
    fun `✅유저 포인트 사용_사용 금액이 잔액에서 차감되어야 한다`() {
        val now = LocalDateTime.now()
        val userPoint = UserPoint(userId = 1L, balance = Money.of(1000))
        userPoint.use(Money.of(1000), now)
        userPoint.balance shouldBe Money.ZERO
        userPoint.updatedAt shouldBe now
    }

    @Test
    fun `⛔️유저 포인트 사용 실패_잔액이 사용 금액보다 적으면 IllegalArgumentException 예외가 발생해야 한다`() {
        val now = LocalDateTime.now()
        val userPoint = UserPoint(userId = 1L, balance = Money.of(999))
        shouldThrowExactly<IllegalArgumentException> { userPoint.use(Money.of(1000), now) }
    }
}