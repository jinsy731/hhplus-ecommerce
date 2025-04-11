package kr.hhplus.be.server.user.domain

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.common.exception.InsufficientPointException
import kr.hhplus.be.server.common.exception.InvalidChargeAmountException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointTest {
    @Test
    fun `✅ 유저 포인트 충전_충전 금액이 잔액에 반영되어야 한다`() {
        // arrange
        val userPoint = UserPoint(1L, 1L, BigDecimal.ZERO)
        val time = LocalDateTime.now()
        // act
        userPoint.charge(BigDecimal(100), time)
        // assert
        userPoint.balance shouldBe BigDecimal(100)
        userPoint.updatedAt shouldBe time
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "0"])
    fun `⛔️유저 포인트 충전 실패_충전 금액이 0보다 작거나 같으면 InvalidChargeAmountException 이 발생해야한다`(amount: BigDecimal) {
        val userPoint = UserPoint(1L, 1L, BigDecimal.ZERO)
        val ex = shouldThrowExactly<InvalidChargeAmountException> { userPoint.charge(amount, LocalDateTime.now()) }
        ex.message shouldBe ErrorCode.INVALID_CHARGE_AMOUNT.message
    }
    
    @Test
    fun `✅유저 포인트 사용_사용 금액이 잔액에서 차감되어야 한다`() {
        val now = LocalDateTime.now()
        val userPoint = UserPoint(userId = 1L, balance = BigDecimal(1000))
        userPoint.use(BigDecimal(1000), now)
        userPoint.balance shouldBe BigDecimal.ZERO
        userPoint.updatedAt shouldBe now
    }

    @Test
    fun `⛔️유저 포인트 사용 실패_잔액이 사용 금액보다 적으면 InsufficientPointException 예외가 발생해야 한다`() {
        val now = LocalDateTime.now()
        val userPoint = UserPoint(userId = 1L, balance = BigDecimal(999))
        shouldThrowExactly<InsufficientPointException> { userPoint.use(BigDecimal(1000), now) }
    }
}