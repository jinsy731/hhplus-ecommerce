package kr.hhplus.be.server.user.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.user.UserPointTestFixture
import kr.hhplus.be.server.user.domain.TransactionType
import kr.hhplus.be.server.user.domain.UserPoint
import kr.hhplus.be.server.user.domain.UserPointHistoryRepository
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class UserPointServiceIntegrationTest {

    @Autowired
    lateinit var userPointService: UserPointService

    @Autowired
    lateinit var userPointRepository: UserPointRepository

    @Autowired
    lateinit var userPointHistoryRepository: UserPointHistoryRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `사용자의 포인트를 충전하면 잔액과 히스토리가 업데이트된다`() {
        // Arrange
        val userId = 1L
        val initialPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = BigDecimal.ZERO)
        userPointRepository.save(initialPoint)

        val now = LocalDateTime.now()
        val chargeAmount = BigDecimal(5000)
        val command = UserPointCommand.Charge(userId, chargeAmount, now)

        // Act
        val result = userPointService.charge(command)

        // Assert
        val updatedPoint = userPointRepository.getByUserId(userId)
        updatedPoint.balance.compareTo(chargeAmount) shouldBe 0
        updatedPoint.updatedAt shouldBe now

        result.userId shouldBe 1L
        result.pointAfterCharge.compareTo(updatedPoint.balance) shouldBe 0
        result.updatedAt shouldBe now

        val historyList = userPointHistoryRepository.findAllByUserId(userId)
        historyList shouldHaveSize 1
        with(historyList.first()) {
            amount.compareTo(chargeAmount) shouldBe 0
            createdAt shouldBe now
            transactionType shouldBe TransactionType.CHARGE
        }
    }

    @Test
    fun `사용자의 포인트를 사용할 수 있다`() {
        // Arrange
        val userId = 2L
        val initialBalance = BigDecimal(10000)
        val initialPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = initialBalance)
        userPointRepository.save(initialPoint)

        val useAmount = BigDecimal(3000)
        val now = LocalDateTime.now()
        val command = UserPointCommand.Use(userId, useAmount, now)

        // Act
        userPointService.use(command)

        // Assert
        val updatedPoint = userPointRepository.getByUserId(userId)
        updatedPoint.balance.compareTo((initialBalance - useAmount)) shouldBe 0
        updatedPoint.updatedAt shouldBe now

        val historyList = userPointHistoryRepository.findAllByUserId(userId)
        historyList shouldHaveSize 1
        with(historyList.first()) {
            amount.compareTo(useAmount) shouldBe 0
            createdAt shouldBe now
            transactionType shouldBe TransactionType.USE
        }
    }

    @Test
    fun `사용자의 포인트를 조회할 수 있다`() {
        // Arrange
        val userId = 3L
        val balance = BigDecimal(7000)
        val now = LocalDateTime.now()
        val userPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = balance, updatedAt = now)
        userPointRepository.save(userPoint)

        val command = UserPointCommand.Retrieve(userId)

        // Act
        val result = userPointService.retrievePoint(command)

        // Assert
        result.userId shouldBe userId
        result.point.compareTo(balance) shouldBe 0
        result.updatedAt shouldBe now
    }
}
