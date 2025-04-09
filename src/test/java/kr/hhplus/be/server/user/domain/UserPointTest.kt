package kr.hhplus.be.server.user.domain

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.ErrorCode
import kr.hhplus.be.server.user.InvalidChargeAmountException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class UserPointTest {
    @Test
    fun `✅ 유저 포인트 충전_충전 금액이 잔액에 반영되어야 한다`() {
        val userPoint = UserPoint(1L, 1L, 0)
        val time = LocalDateTime.now()
        val newUserPoint = userPoint.charge(100L, time)
        newUserPoint.balance shouldBe 100L
        newUserPoint.updatedAt shouldBe time
    }

    @ParameterizedTest
    @ValueSource(longs = [-1L, 0L])
    fun `⛔️유저 포인트 충전 실패_충전 금액이 0보다 작거나 같으면 InvalidChargeAmountException 이 발생해야한다`(amount: Long) {
        val userPoint = UserPoint(1L, 1L, 0L)
        val ex = shouldThrowExactly<InvalidChargeAmountException> { userPoint.charge(amount, LocalDateTime.now()) }
        ex.message shouldBe ErrorCode.INVALID_CHARGE_AMOUNT.message
    }
}