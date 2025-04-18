package kr.hhplus.be.server.user.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.common.exception.InsufficientPointException
import kr.hhplus.be.server.common.exception.InvalidChargeAmountException
import kr.hhplus.be.server.user.domain.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class UserPointServiceTest {
    private lateinit var userPointRepository: UserPointRepository
    private lateinit var userPointHistoryRepository: UserPointHistoryRepository
    private lateinit var pointService: UserPointService

    @BeforeEach
    fun setUp() {
        userPointRepository = mockk()
        userPointHistoryRepository = mockk()
        pointService = UserPointService(userPointRepository, userPointHistoryRepository)
    }

    @Test
    fun `✅ 유저 포인트 충전`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = BigDecimal(100)
        val chargeAmount = BigDecimal(100)
        val finalBalance = initialBalance + chargeAmount
        
        val userPoint = UserPoint(1L, userId, initialBalance).apply { this.createdAt = time }

        val expectedHistory = UserPointHistory(
            userId = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            createdAt = time
        )
        
        every { userPointRepository.getByUserId(userId) } returns userPoint
        every { userPointRepository.save(any()) } returns userPoint
        every { userPointHistoryRepository.save(any()) } returns expectedHistory
        
        val cmd = UserPointCommand.Charge(
            userId = userId,
            amount = chargeAmount,
            now = time
        )

        //act
        val result = pointService.charge(cmd)

        //assert
        result.userId shouldBe 1L
        result.pointAfterCharge shouldBe finalBalance
        result.updatedAt shouldBe time
        verify(exactly = 1) { userPointRepository.save(userPoint) }
        verify(exactly = 1) {
            userPointHistoryRepository.save(match {
                it.userId == userId &&
                        it.amount == chargeAmount &&
                        it.transactionType == TransactionType.CHARGE &&
                        it.createdAt == time
            })
        }
    }
    
    @Test
    fun `⛔️ 유저 포인트 충전 실패_유저 포인트와 포인트 내역이 저장되지 않아야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = BigDecimal(100)
        val chargeAmount = BigDecimal.ZERO

        val userPoint = UserPoint(1L, userId, initialBalance)

        every { userPointRepository.getByUserId(userId) } returns userPoint

        val cmd = UserPointCommand.Charge(
            userId = userId,
            amount = chargeAmount,
            now = time
        )

        //act
        shouldThrowExactly<InvalidChargeAmountException> { pointService.charge(cmd) }

        //assert
        verify(exactly = 0) { userPointRepository.save(any()) }
        verify(exactly = 0) { userPointHistoryRepository.save(any()) }
    }

    @Test
    fun `✅ 유저 포인트 사용`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = BigDecimal(100)
        val useAmount = BigDecimal(100)
        val finalBalance = initialBalance - useAmount

        val userPoint = UserPoint(1L, userId, initialBalance).apply { this.createdAt = time }

        val expectedHistory = UserPointHistory(
            userId = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            createdAt = time
        )

        every { userPointRepository.getByUserId(userId) } returns userPoint
        every { userPointRepository.save(any()) } returns userPoint
        every { userPointHistoryRepository.save(any()) } returns expectedHistory

        val cmd = UserPointCommand.Use(
            userId = userId,
            amount = useAmount,
            now = time
        )

        //act
        pointService.use(cmd)

        //assert
        userPoint.balance shouldBe finalBalance
        verify(exactly = 1) { userPointRepository.save(userPoint) }
        verify(exactly = 1) {
            userPointHistoryRepository.save(match {
                it.userId == userId &&
                        it.amount == useAmount &&
                        it.transactionType == TransactionType.USE &&
                        it.createdAt == time
            })
        }
    }

    @Test
    fun `⛔️ 유저 포인트 사용 실패_유저 포인트와 포인트 내역이 저장되지 않아야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val userId = 1L
        val initialBalance = BigDecimal(100)
        val useAmount = BigDecimal(101)

        val userPoint = UserPoint(1L, userId, initialBalance).apply { this.createdAt = time }

        every { userPointRepository.getByUserId(userId) } returns userPoint

        val cmd = UserPointCommand.Use(
            userId = userId,
            amount = useAmount,
            now = time
        )

        //act
        shouldThrowExactly<InsufficientPointException> { pointService.use(cmd) }

        //assert
        userPoint.balance shouldBe initialBalance
        verify(exactly = 0) { userPointRepository.save(any()) }
        verify(exactly = 0) { userPointHistoryRepository.save(any()) }
    }
    
    @Test
    fun `✅유저 포인트 조회`() {
        // arrange
        val userId = 1L
        val balance = BigDecimal(100)
        val time = LocalDateTime.now()
        val userPoint = UserPoint(1L, userId, balance).apply { this.updatedAt = time }
        val cmd = UserPointCommand.Retrieve(userId)
        every { userPointRepository.getByUserId(1L) } returns userPoint

        // act
        val result = pointService.retrievePoint(cmd)

        // assert
        result.userId shouldBe 1L
        result.point shouldBe balance
        result.updatedAt shouldBe time
    }
}