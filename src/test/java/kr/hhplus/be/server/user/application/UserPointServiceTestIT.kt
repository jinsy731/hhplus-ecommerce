package kr.hhplus.be.server.user.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.user.domain.UserPoint
import kr.hhplus.be.server.user.domain.UserPointHistoryRepository
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTestWithMySQLContainer
class UserPointServiceIntegrationTest {

    @Autowired
    lateinit var userPointService: UserPointService

    @Autowired
    lateinit var userPointRepository: UserPointRepository

    @Autowired
    lateinit var userPointHistoryRepository: UserPointHistoryRepository

    @Test
    fun `사용자의 포인트를 충전하면 잔액과 히스토리가 업데이트된다`() {
        // Arrange
        val userId = 1L
        val initialPoint = UserPoint(userId = userId, balance = BigDecimal.ZERO, updatedAt = LocalDateTime.now())
        userPointRepository.save(initialPoint)

        val now = LocalDateTime.now()
        val chargeAmount = BigDecimal(5000)
        val command = UserPointCommand.Charge(userId, chargeAmount, now)

        // Act
        val result = userPointService.charge(command)

        // Assert
        val updatedPoint = userPointRepository.getByUserId(userId)
        updatedPoint.balance shouldBe chargeAmount
        updatedPoint.updatedAt shouldBe now

        val historyList = userPointHistoryRepository.findAllByUserId(userId)
        historyList shouldHaveSize 1
        with(historyList.first()) {
            amount shouldBe chargeAmount
            createdAt shouldBe now
            transactionType shouldBe "CHARGE"
        }
    }

    @Test
    fun `사용자의 포인트를 사용할 수 있다`() {
        // Arrange
        val userId = 2L
        val initialBalance = BigDecimal(10000)
        val now = LocalDateTime.now()
        val initialPoint = UserPoint(userId = userId, balance = initialBalance, updatedAt = now)
        userPointRepository.save(initialPoint)

        val useAmount = BigDecimal(3000)
        val command = UserPointCommand.Use(userId, useAmount, now)

        // Act
        userPointService.use(command)

        // Assert
        val updatedPoint = userPointRepository.getByUserId(userId)
        updatedPoint.balance shouldBe (initialBalance - useAmount)
        updatedPoint.updatedAt shouldBe now

        val historyList = userPointHistoryRepository.findAllByUserId(userId)
        historyList shouldHaveSize 1
        with(historyList.first()) {
            amount shouldBe useAmount
            createdAt shouldBe now
            transactionType shouldBe "USE"
        }
    }

    @Test
    fun `사용자의 포인트를 조회할 수 있다`() {
        // Arrange
        val userId = 3L
        val balance = BigDecimal(7000)
        val now = LocalDateTime.now()
        val userPoint = UserPoint(userId = userId, balance = balance, updatedAt = now)
        userPointRepository.save(userPoint)

        val command = UserPointCommand.Retrieve(userId)

        // Act
        val result = userPointService.retrievePoint(command)

        // Assert
        result.userId shouldBe userId
        result.point shouldBe balance
        result.updatedAt shouldBe now
    }
}
