package kr.hhplus.be.server.user.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserPointHistoryTest {
    
    @Test
    fun `✅ 유저 포인트 내역 생성_충전`() {
        val time = LocalDateTime.now()
        val chargeHistory = UserPointHistory.createChargeHistory(1L, 100L, time)
        chargeHistory.userId shouldBe 1L
        chargeHistory.amount shouldBe 100L
        chargeHistory.transactionType shouldBe TransactionType.CHARGE
        chargeHistory.updatedAt shouldBe time
    }

    @Test
    fun `✅ 유저 포인트 내역 생성_사용`() {
        val time = LocalDateTime.now()
        val chargeHistory = UserPointHistory.createUseHistory(1L, 100L, time)
        chargeHistory.userId shouldBe 1L
        chargeHistory.amount shouldBe 100L
        chargeHistory.transactionType shouldBe TransactionType.USE
        chargeHistory.updatedAt shouldBe time
    }
}