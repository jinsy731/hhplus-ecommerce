package kr.hhplus.be.server.user.application

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.common.exception.InsufficientPointException
import kr.hhplus.be.server.user.UserPointTestFixture
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTestWithMySQLContainer
class UserPointServiceConcurrencyTestIT {

    @Autowired
    private lateinit var userPointService: UserPointService

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    private val userId = 1L
    private val initialBalance = BigDecimal(10000)

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 포인트 생성
        val userPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = initialBalance)
        userPointRepository.save(userPoint)
    }

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `여러 스레드에서 동시에 포인트를 사용하면 일부는 포인트 부족으로 실패한다`() {
        // 동시에 실행할 스레드 수
        val threadCount = 15
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val useAmount = BigDecimal(1000)

        // 초기 잔액이 10000원이고, 각 요청은 1000원씩 15개이므로 일부는 실패해야 함
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    userPointService.use(
                        UserPointCommand.Use(
                            userId = userId,
                            amount = useAmount,
                            now = LocalDateTime.now()
                        )
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    if (e is InsufficientPointException) {
                        failCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 검증
        successCount.get() + failCount.get() shouldBe threadCount
        successCount.get() shouldBe 10 // 10000원 / 1000원 = 10건 성공
        failCount.get() shouldBe 5 // 15건 중 5건 실패

        // 최종 잔액 확인
        val finalUserPoint = userPointRepository.getByUserId(userId)
        finalUserPoint.balance shouldBe BigDecimal.ZERO // 10000원 - (1000원 * 10건) = 0원
    }

    @Test
    fun `여러 스레드에서 동시에 포인트를 충전하면 모두 정상적으로 반영된다`() {
        // 초기 잔액 0원으로 설정
        val userPoint = userPointRepository.getByUserId(userId)
        userPoint.balance = BigDecimal.ZERO
        userPointRepository.save(userPoint)

        // 동시에 실행할 스레드 수
        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val chargeAmount = BigDecimal(1000)

        // 10개의 스레드에서 동시에 1000원씩 충전
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    userPointService.charge(
                        UserPointCommand.Charge(
                            userId = userId,
                            amount = chargeAmount,
                            now = LocalDateTime.now()
                        )
                    )
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 최종 잔액 확인 (모든 충전이 반영되어야 함)
        val finalUserPoint = userPointRepository.getByUserId(userId)
        finalUserPoint.balance shouldBe chargeAmount.multiply(BigDecimal(threadCount))
    }

    @Test
    fun `포인트 충전과 사용이 동시에 발생할 때 동시성 제어 테스트`() {
        // 초기 잔액 0원으로 설정
        val userPoint = userPointRepository.getByUserId(userId)
        userPoint.balance = BigDecimal.ZERO
        userPointRepository.save(userPoint)

        val chargeThreadCount = 5 // 충전 스레드 수
        val useThreadCount = 10 // 사용 스레드 수
        val totalThreadCount = chargeThreadCount + useThreadCount
        
        val executorService = Executors.newFixedThreadPool(totalThreadCount)
        val latch = CountDownLatch(totalThreadCount)
        
        val chargeAmount = BigDecimal(2000) // 각 충전액
        val useAmount = BigDecimal(1000) // 각 사용액
        
        val chargeSuccessCount = AtomicInteger(0)
        val useSuccessCount = AtomicInteger(0)
        val useFailCount = AtomicInteger(0)

        // 충전 스레드 실행
        for (i in 1..chargeThreadCount) {
            executorService.execute {
                try {
                    userPointService.charge(
                        UserPointCommand.Charge(
                            userId = userId,
                            amount = chargeAmount,
                            now = LocalDateTime.now().plusNanos((Math.random() * 1000000).toLong())
                        )
                    )
                    chargeSuccessCount.incrementAndGet()
                } catch (e: Exception) {
                    // 충전은 실패하지 않을 것으로 예상
                } finally {
                    latch.countDown()
                }
            }
        }

        // 사용 스레드 실행 (사용량이 충전량보다 큰 경우)
        for (i in 1..useThreadCount) {
            executorService.execute {
                try {
                    // 약간의 지연을 줘서 일부 충전이 먼저 이루어지도록 함
                    Thread.sleep((Math.random() * 10).toLong())
                    
                    userPointService.use(
                        UserPointCommand.Use(
                            userId = userId,
                            amount = useAmount,
                            now = LocalDateTime.now().plusNanos((Math.random() * 1000000).toLong())
                        )
                    )
                    useSuccessCount.incrementAndGet()
                } catch (e: Exception) {
                    if (e is InsufficientPointException) {
                        useFailCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 검증
        chargeSuccessCount.get() shouldBe chargeThreadCount // 모든 충전은 성공해야 함
        useSuccessCount.get() + useFailCount.get() shouldBe useThreadCount // 사용 성공과 실패의 합은 전체 사용 스레드 수와 같아야 함
        
        // 최종 잔액 확인
        val finalUserPoint = userPointRepository.getByUserId(userId)
        val expectedBalance = (chargeAmount.multiply(BigDecimal(chargeThreadCount)))
            .subtract(useAmount.multiply(BigDecimal(useSuccessCount.get())))
        
        finalUserPoint.balance shouldBe expectedBalance
    }

    @Test
    fun `대량의 포인트 충전 요청 시 동시성 문제가 발생하지 않는다`() {
        // 초기 잔액 0원으로 설정
        val userPoint = userPointRepository.getByUserId(userId)
        userPoint.balance = BigDecimal.ZERO
        userPointRepository.save(userPoint)

        // 동시에 실행할 스레드 수 (대량)
        val threadCount = 50
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val chargeAmount = BigDecimal(100)

        // 50개의 스레드에서 동시에 100원씩 충전
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    userPointService.charge(
                        UserPointCommand.Charge(
                            userId = userId,
                            amount = chargeAmount,
                            now = LocalDateTime.now().plusNanos(i.toLong())
                        )
                    )
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 최종 잔액 확인 (모든 충전이 반영되어야 함)
        val finalUserPoint = userPointRepository.getByUserId(userId)
        finalUserPoint.balance shouldBe chargeAmount.multiply(BigDecimal(threadCount))
    }

    @Test
    fun `소액의 포인트를 많은 스레드에서 동시에 사용할 때 정확한 잔액 차감 확인`() {
        // 초기 잔액 설정
        val initialAmount = BigDecimal(10000)
        val userPoint = userPointRepository.getByUserId(userId)
        userPoint.balance = initialAmount
        userPointRepository.save(userPoint)

        // 동시에 실행할 스레드 수
        val threadCount = 100
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val useAmount = BigDecimal(100) // 소액
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // 100개의 스레드에서 동시에 100원씩 사용
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    userPointService.use(
                        UserPointCommand.Use(
                            userId = userId,
                            amount = useAmount,
                            now = LocalDateTime.now().plusNanos(i.toLong())
                        )
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 검증
        successCount.get() shouldBe threadCount // 모든 사용 요청이 성공해야 함 (잔액 충분)
        failCount.get() shouldBe 0 // 실패는 없어야 함

        // 최종 잔액 확인
        val finalUserPoint = userPointRepository.getByUserId(userId)
        finalUserPoint.balance shouldBe initialAmount.subtract(useAmount.multiply(BigDecimal(threadCount)))
    }
}