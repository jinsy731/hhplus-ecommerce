package kr.hhplus.be.server.point.application

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.executeConcurrently
import kr.hhplus.be.server.executeMultipleFunctionConcurrently
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.domain.UserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class UserPointServiceConcurrencyTestIT {

    @Autowired
    private lateinit var userPointService: UserPointService

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
    }


    @Test
    fun `✅포인트 사용 동시성 테스트_100개의 포인트 사용 요청이 동시에 발생할 때 일부만 성공하고 성공한만큼 잔액에서 차감되어야 한다`() {
        // arrange: 초기 유저 포인트는 10만
        val userId = 1L
        val successCnt = AtomicInteger(0)
        val failureCnt = AtomicInteger(0)
        val useAmount = Money.of(1000)
        userPointRepository.save(UserPointTestFixture
            .userPoint(userId = userId, balance = Money.of(100000))
            .build())

        // act: 100개의 요청을 동시 실행
        executeConcurrently(count = 100) {
            try {
                userPointService.use(UserPointCommand.Use(userId, useAmount))
                successCnt.incrementAndGet()
            } catch(e: OptimisticLockingFailureException) {
                failureCnt.incrementAndGet()
            }
        }

        // assert: 성공 횟수 * 1000 만큼 잔액이 차감되어야 한다.
        val resultUserPoint = userPointRepository.getByUserId(userId)
        resultUserPoint.balance shouldBe Money.of(100000) - Money.of(1000 * successCnt.get().toLong())
    }

    @Test
    fun `✅포인트 충전 동시성 테스트_100개의 포인트 충전 요청이 동시에 발생할 때 일부만 성공하고 성공한만큼 잔액이 증가해야한다`() {
        // arrange: 초기 유저 포인트는 0
        val userId = 1L
        val successCnt = AtomicInteger(0)
        val failureCnt = AtomicInteger(0)
        val chargeAmount = Money.of(1000)
        userPointRepository.save(UserPointTestFixture
            .userPoint(userId = userId, balance = Money.of(0))
            .build())

        // act: 100개의 요청을 동시 실행
        executeConcurrently(100) {
            try {
                userPointService.charge(UserPointCommand.Charge(userId, chargeAmount))
                successCnt.incrementAndGet()
            } catch(e: OptimisticLockingFailureException) {
                failureCnt.incrementAndGet()
            }
        }

        // assert: 성공 횟수 * 1000 만큼 잔액이 증가해야 한다.
        val resultUserPoint = userPointRepository.getByUserId(userId)
        resultUserPoint.balance shouldBe Money.of(successCnt.get().toLong() * 1000)
    }
    
    @Test
    fun `✅포인트 사용&충전 동시성 테스트_100개의 포인트 사용&충전 요청이 동시에 발생할 때 일부만 성공하고 성공한 만큼 잔액이 변경되어야 한다`() {
        // arrange: 초기 유저 포인트 10만
        val userId = 1L
        val useSuccessCnt = AtomicInteger(0)
        val useFailureCnt = AtomicInteger(0)
        val chargeSuccessCnt = AtomicInteger(0)
        val chargeFailureCnt = AtomicInteger(0)
        val useAmount = Money.of(1000)
        val chargeAmount = Money.of(1000)
        userPointRepository.save(UserPointTestFixture
            .userPoint(userId = userId, balance = Money.of(100000))
            .build())

        // act: 100개의 충전/사용 요청을 동시 실행
        executeMultipleFunctionConcurrently(100, listOf(
            {
                try {
                    userPointService.charge(UserPointCommand.Charge(userId, chargeAmount))
                    chargeSuccessCnt.incrementAndGet()
                } catch (e: OptimisticLockingFailureException) {
                    chargeFailureCnt.incrementAndGet()
                }
            },
            {
                try {
                    userPointService.use(UserPointCommand.Use(userId, useAmount))
                } catch (e: OptimisticLockingFailureException) {
                    useFailureCnt.incrementAndGet()
                }
            }
        ))
        
        // assert: 충전 성공횟수 * 1000만큼 잔액이 증가하고, 사용 성공횟수 * 1000만큼 잔액이 감소해야 한다.
        val balanceDiff = (chargeSuccessCnt.get().toLong() * 1000) - (useSuccessCnt.get().toLong() * 1000)
        val resultUserPoint = userPointRepository.getByUserId(userId)
        resultUserPoint.balance shouldBe Money.of(100000 - balanceDiff)
    }
}