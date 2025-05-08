package kr.hhplus.be.server.user.domain

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserPointHistoryTest {
    
    @Test
    fun `✅ 유저 포인트 내역 생성_충전`() {
        val time = LocalDateTime.now()
        val chargeHistory = UserPointHistory.createChargeHistory(1L, Money.of(100), time)
        chargeHistory.userId shouldBe 1L
        chargeHistory.amount shouldBe Money.of(100)
        chargeHistory.transactionType shouldBe TransactionType.CHARGE
        chargeHistory.createdAt shouldBe time
    }

    @Test
    fun `✅ 유저 포인트 내역 생성_사용`() {
        val time = LocalDateTime.now()
        val chargeHistory = UserPointHistory.createUseHistory(1L, Money.of(100), time)
        chargeHistory.userId shouldBe 1L
        chargeHistory.amount shouldBe Money.of(100)
        chargeHistory.transactionType shouldBe TransactionType.USE
        chargeHistory.createdAt shouldBe time
    }
}