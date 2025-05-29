package kr.hhplus.be.server.point.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.domain.UserPointHistoryRepository
import kr.hhplus.be.server.point.domain.UserPointRepository
import kr.hhplus.be.server.point.domain.model.TransactionType
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
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

    @MockitoBean
    private lateinit var eventPublisher: DomainEventPublisher

    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
    }

    @Test
    fun `사용자의 포인트를 충전하면 잔액과 히스토리가 업데이트된다`() {
        // Arrange
        val userId = 1L
        val initialPoint = UserPointTestFixture.userPoint(userId = userId, balance = Money.ZERO).build()
        userPointRepository.save(initialPoint)

        val now = LocalDateTime.now()
        val chargeAmount = Money.of(5000)
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
        val orderId = 1L
        val userId = 2L
        val initialBalance = Money.of(10000)
        val initialPoint = UserPointTestFixture.userPoint(userId = userId, balance = initialBalance).build()
        userPointRepository.save(initialPoint)

        val useAmount = Money.of(3000)
        val now = LocalDateTime.now()
        val command = UserPointCommand.Use(
            userId = userId,
            amount = useAmount,
            orderId = orderId,
            now = now
        )

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
        val balance = Money.of(7000)
        val now = LocalDateTime.now()
        val userPoint = UserPointTestFixture.userPoint(userId = userId, balance = balance, updatedAt = now).build()
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
