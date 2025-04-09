package kr.hhplus.be.server.user.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.user.InvalidChargeAmountException
import kr.hhplus.be.server.user.domain.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserPointServiceTest {
    private val userPointRepository: UserPointRepository = mockk()
    private val userPointHistoryRepository: UserPointHistoryRepository = mockk()
    private val pointService = UserPointService(userPointRepository, userPointHistoryRepository)

    @Test
    fun `✅ 유저 포인트 충전`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = 100L
        val chargeAmount = 100L
        val finalBalance = initialBalance + chargeAmount
        
        val userPoint = UserPoint(1L, userId, initialBalance)
        val expectedUserPoint = UserPoint(1L, userId, finalBalance)
        val savedUserPoint = UserPoint(1L, userId, finalBalance).apply { this.updatedAt = time }
        
        val expectedHistory = UserPointHistory(
            userId = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
        ).apply { this.updatedAt = time }
        
        every { userPointRepository.getByUserId(userId) } returns userPoint
        every { userPointRepository.save(expectedUserPoint) } returns savedUserPoint
        every { userPointHistoryRepository.save(expectedHistory) } returns mockk<UserPointHistory>()
        
        val cmd = UserPointCommand.Charge(
            userId = userId,
            amount = chargeAmount,
            time = time
        )

        //act
        val result = pointService.charge(cmd)

        //assert
        result.pointAfterCharge shouldBe 200L
        result.updatedAt shouldBe time
        verify(exactly = 1) { userPointRepository.save(expectedUserPoint) }
        verify(exactly = 1) { userPointHistoryRepository.save(expectedHistory) }
    }
    
    @Test
    fun `⛔️ 유저 포인트 충전 실패_유저 포인트와 포인트 내역이 저장되지 않아야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = 100L
        val chargeAmount = 0L

        val userPoint = UserPoint(1L, userId, initialBalance)

        every { userPointRepository.getByUserId(userId) } returns userPoint

        val cmd = UserPointCommand.Charge(
            userId = userId,
            amount = chargeAmount,
            time = time
        )

        //act
        shouldThrowExactly<InvalidChargeAmountException> { pointService.charge(cmd) }

        //assert
        verify(exactly = 0) { userPointRepository.save(any()) }
        verify(exactly = 0) { userPointHistoryRepository.save(any()) }
    }
}