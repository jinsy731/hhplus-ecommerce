package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponIssueRequest
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponStock
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaUserCouponRepository
import kr.hhplus.be.server.executeConcurrently
import kr.hhplus.be.server.order.application.OrderSagaContext
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import kr.hhplus.be.server.shared.exception.CouponOutOfStockException
import kr.hhplus.be.server.shared.exception.DuplicateCouponIssueException
import kr.hhplus.be.server.shared.time.ClockHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class CouponServiceTestIT @Autowired constructor(
    private val couponService: CouponService,
    private val couponIssueBatchService: CouponIssueBatchService,
    private val couponRepository: CouponRepository,
    private val userCouponJpaRepository: JpaUserCouponRepository,
    @MockitoSpyBean private val userCouponRepository: UserCouponRepository,
    private val couponKVStore: CouponKVStore,
    private val redisCleaner: RedisCleaner,
    @MockitoBean private val mockClockHolder: ClockHolder,
    @MockitoBean private val eventPublisher: DomainEventPublisher,
    private val databaseCleaner: MySqlDatabaseCleaner,
){
    private val logger = LoggerFactory.getLogger(javaClass)

    @BeforeEach
    fun setup() {
        redisCleaner.clean()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(LocalDateTime.now())
    }

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
        redisCleaner.clean()
    }

    @Test
    fun `✅쿠폰 발급을 하면 사용자 쿠폰이 생성된다`() {
        // given
        val userId = 1L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )

        // when
        val result = couponService.issueCoupon(issueCommand)

        // then
        result.userCouponId shouldNotBe null
        result.status shouldBe UserCouponStatus.UNUSED
        result.expiredAt shouldBe now.plusDays(10)

        // DB에 저장된 사용자 쿠폰 확인
        val userCoupon = userCouponRepository.findById(result.userCouponId!!) ?: throw IllegalStateException()
        userCoupon.userId shouldBe userId
        userCoupon.coupon.id shouldBe couponId
        userCoupon.status shouldBe UserCouponStatus.UNUSED

        // 쿠폰의 발급 횟수가 증가했는지 확인
        val updatedCoupon = couponRepository.getById(couponId)
        updatedCoupon.issuedCount shouldBe 1
    }

    @Test
    fun `✅비동기 쿠폰 발급을 요청하고 배치 처리하면 사용자 쿠폰이 생성된다`() {
        // given
        val userId = 100L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )

        // when
        // 1. 비동기 쿠폰 발급 요청
        val result = couponService.issueCouponAsync(issueCommand)

        // 2. 배치 서비스 실행하여 실제 쿠폰 발급 처리
        couponIssueBatchService.processIssueRequest()

        // then
        // 쿠폰 발급 결과 확인
        result.couponId shouldBe couponId
        result.status shouldBe IssuedStatus.PENDING.name

        // 사용자 쿠폰이 실제로 발급되었는지 확인
        val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
        userCoupon shouldNotBe null
        userCoupon!!.userId shouldBe userId
        userCoupon.coupon.id shouldBe couponId
        userCoupon.status shouldBe UserCouponStatus.UNUSED

        // Redis에 발급 완료로 표시되었는지 확인
        couponKVStore.existsIssuedUser(userId, couponId) shouldBe true

        // 큐에서 요청이 제거되었는지 확인
        val requests = couponKVStore.peekAllFromIssueRequestQueue(couponId)
        requests shouldHaveSize 0
    }

    @Test
    fun `⛔️이미 발급 요청된 쿠폰은 중복 발급할 수 없다`() {
        // given
        val userId = 200L
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        // Redis에 발급된 사용자로 표시
        couponKVStore.setIssuedUser(userId, couponId)

        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )

        // when & then
        shouldThrow<DuplicateCouponIssueException> {
            couponService.issueCouponAsync(issueCommand)
        }
    }

    @Test
    fun `⛔️쿠폰 재고가 소진되면 발급할 수 없다`() {
        // given
        val userId = 300L
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정 (소진 상태)
        couponKVStore.setStock(CouponStock(couponId, 10))

        // 이미 10명이 발급받은 상태로 설정
        repeat(10) { i ->
            couponKVStore.setIssuedUser(i.toLong(), couponId)
        }

        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )

        // when & then
        shouldThrow<CouponOutOfStockException> {
            couponService.issueCouponAsync(issueCommand)
        }
    }

    @Test
    fun `✅동일 유저가 동일 쿠폰에 대해 동시에 여러 요청을 보내면 중복 발급 방지가 작동한다`() {
        // given
        val userId = 400L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        // when
        val batchRunCount = 5
        val latch = CountDownLatch(1)
        val batchCompleteLatch = CountDownLatch(batchRunCount)

        // 배치 프로세스를 실행할 스레드 생성
        val batchExecutor = Executors.newSingleThreadExecutor()
        batchExecutor.submit {
            try {
                // 메인 스레드에서 요청 실행이 시작될 때까지 대기
                latch.await()

                // 배치 프로세스를 여러 번 실행
                repeat(batchRunCount) {
                    logger.info { "[${Thread.currentThread().name}] batch run" }
                    couponIssueBatchService.processIssueRequest()
                    Thread.sleep(100) // 약간의 지연 추가
                    batchCompleteLatch.countDown()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        executeConcurrently(100) {
            if(it == 0) { latch.countDown() }
            val issueCommand = CouponCommand.Issue(
                userId = userId,
                couponId = couponId
            )
            couponService.issueCouponAsync(issueCommand)
        }

        batchCompleteLatch.await()
        batchExecutor.shutdown()

        // then
        // 배치 처리 후 DB에 사용자 쿠폰이 하나만 생성되었는지 확인
        val userCoupons = userCouponRepository.findAllByUserId(userId, PageRequest.of(0, 10))
        userCoupons.content.size shouldBe 1
        userCoupons.content[0].userId shouldBe userId
        userCoupons.content[0].coupon.id shouldBe couponId

        // Redis에 발급 완료로 표시되었는지 확인
        couponKVStore.existsIssuedUser(userId, couponId) shouldBe true
    }

    @Test
    fun `✅PENDING 상태의 비동기 쿠폰 발급 상태를 조회한다`() {
        // given
        val userId = 1000L
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        // 비동기 쿠폰 발급 요청
        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )
        couponService.issueCouponAsync(issueCommand)

        // when
        val result = couponService.getIssueStatus(userId, couponId)

        // then
        result.couponId shouldBe couponId
        result.status shouldBe IssuedStatus.PENDING.name
        result.userCouponId shouldBe null
    }

    @Test
    fun `✅ISSUED 상태의 비동기 쿠폰 발급 상태를 조회한다`() {
        // given
        val userId = 1001L
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        // 비동기 쿠폰 발급 요청
        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )
        couponService.issueCouponAsync(issueCommand)

        // 배치 서비스 실행으로 실제 발급 처리
        couponIssueBatchService.processIssueRequest()

        // when
        val result = couponService.getIssueStatus(userId, couponId)

        // then
        result.couponId shouldBe couponId
        result.status shouldBe IssuedStatus.ISSUED.name
        result.userCouponId shouldNotBe null

        // DB에 저장된 사용자 쿠폰 확인
        val userCoupon = userCouponRepository.findById(result.userCouponId!!)
        userCoupon shouldNotBe null
        userCoupon!!.userId shouldBe userId
        userCoupon.coupon.id shouldBe couponId
    }

    @Test
    fun `✅FAILED 상태의 비동기 쿠폰 발급 상태를 조회한다`() {
        // given
        val userId = 1002L
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정 (소진 상태로 만들기 위해 재고 0)
        couponKVStore.setStock(CouponStock(couponId, 0))

        // 비동기 쿠폰 발급 요청을 생성하고 직접 FAILED 상태로 설정
        couponKVStore.setIssuedStatus(userId, couponId, IssuedStatus.FAILED)

        // when
        val result = couponService.getIssueStatus(userId, couponId)

        // then
        result.couponId shouldBe couponId
        result.status shouldBe IssuedStatus.FAILED.name
        result.userCouponId shouldBe null
    }

    @Test
    fun `✅병렬 요청 처리와 배치 프로세스가 동시에 실행될 때 정상 작동한다`() {
        // given
        val userCount = 50
        val batchRunCount = 5
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        val latch = CountDownLatch(1)
        val batchCompleteLatch = CountDownLatch(batchRunCount)

        // 배치 프로세스를 실행할 스레드 생성
        val batchExecutor = Executors.newSingleThreadExecutor()
        batchExecutor.submit {
            try {
                // 메인 스레드에서 요청 실행이 시작될 때까지 대기
                latch.await()

                // 배치 프로세스를 여러 번 실행
                repeat(batchRunCount) {
                    logger.info { "[${Thread.currentThread().name}] batch run" }
                    couponIssueBatchService.processIssueRequest()
                    Thread.sleep(100) // 약간의 지연 추가
                    batchCompleteLatch.countDown()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // when
        // 1. 여러 사용자가 동시에 쿠폰 발급 요청
        executeConcurrently(userCount) { index ->
            try {
                val userId = 1000L + index
                val issueCommand = CouponCommand.Issue(
                    userId = userId,
                    couponId = couponId
                )

                // 첫 번째 요청 전에 latch를 카운트다운하여 배치 프로세스 스레드를 시작
                if (index == 0) {
                    latch.countDown()
                }

                logger.info { "[${Thread.currentThread().name}] issue request" }
                couponService.issueCouponAsync(issueCommand)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 배치 프로세스가 완료될 때까지 대기
        batchCompleteLatch.await(10, TimeUnit.SECONDS)
        batchExecutor.shutdown()

        // 추가로 배치 프로세스를 한 번 더 실행하여 남은 요청 처리
        couponIssueBatchService.processIssueRequest()

        // then
        // 실제로 발급된 쿠폰 수 확인
        val requests = couponKVStore.popBatchFromIssueRequestQueue(couponId, 10000)
        println("requests.size = ${requests.size}")
        val issuedUserCoupons = userCouponJpaRepository.findAll()
        issuedUserCoupons.size shouldBe userCount

        // 큐에 남은 요청이 없는지 확인
        val remainingRequests = couponKVStore.countIssueRequestQueue(couponId)
        remainingRequests shouldBe 0

        // 모든 사용자가 쿠폰을 받았는지 확인
        (0 until userCount).forEach { index ->
            val userId = 1000L + index
            val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
            userCoupon shouldNotBe null
            userCoupon!!.userId shouldBe userId
            userCoupon.coupon.id shouldBe couponId
            userCoupon.status shouldBe UserCouponStatus.UNUSED

            // Redis에 발급 완료로 표시되었는지 확인
            couponKVStore.existsIssuedUser(userId, couponId) shouldBe true
        }
    }

    @Test
    fun `✅비동기 쿠폰 발급 실패 후 재처리 시 성공한다`() {
        // given
        val userId = 600L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!

        // Redis 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(couponId, 100))

        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )

        // 1. 비동기 쿠폰 발급 요청
        couponService.issueCouponAsync(issueCommand)

        // 2. 수동으로 실패 큐에 추가 (실패 시나리오 재현)
        val request = CouponIssueRequest(couponId, userId)
        couponKVStore.pushToFailedIssueRequestQueue(request)
        couponKVStore.pushToFailedIssueRequestedCouponIdList(couponId)

        // 3. 실패 재처리 배치 실행
        couponIssueBatchService.processFailedIssueRequest()

        // then
        // 사용자 쿠폰이 실제로 발급되었는지 확인
        val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
        userCoupon shouldNotBe null
        userCoupon!!.userId shouldBe userId
        userCoupon.coupon.id shouldBe couponId
        userCoupon.status shouldBe UserCouponStatus.UNUSED

        // Redis에 발급 완료로 표시되었는지 확인
        couponKVStore.existsIssuedUser(userId, couponId) shouldBe true
    }

    @Test
    fun `✅쿠폰 재고 소진 시 남은 요청은 실패 처리된다`() {
        // given
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!
        val stockLimit = 5L

        // Redis 쿠폰 재고 설정 (5개 제한)
        couponKVStore.setStock(CouponStock(couponId, stockLimit))

        // 10명이 쿠폰 발급 요청 (재고는 5개)
        repeat(10) { index ->
            val userId = 700L + index
            val issueCommand = CouponCommand.Issue(userId = userId, couponId = couponId)
            couponService.issueCouponAsync(issueCommand)
        }

        // 1. 배치 서비스 실행하여 실제 쿠폰 발급 처리
        couponIssueBatchService.processIssueRequest()

        // then
        // DB에 사용자 쿠폰이 재고 수량만큼만 생성되었는지 확인
        val issuedUserCoupons = userCouponJpaRepository.findAll()
        issuedUserCoupons.size shouldBe stockLimit.toInt()

        // 요청 상태가 FAILED로 변경되었는지 확인
        val failedUserIds = (5..9).map { 700L + it }
        failedUserIds.forEach { userId ->
            val status = couponKVStore.getIssuedStatus(userId, couponId)
            status shouldBe IssuedStatus.FAILED
        }
    }

    @Test
    fun `✅쿠폰 사용_할인 정보가 반환된다`() {
        // given
        val userId = 2L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 쿠폰 생성 및 저장
        val discountPolicy = CouponTestFixture.fixedAmountDiscountPolicy(Money.of(5000))
        val coupon = CouponTestFixture.coupon(discountPolicy = discountPolicy).build()
        val savedCoupon = couponRepository.save(coupon)
        val couponId = savedCoupon.id!!
        
        // 사용자 쿠폰 발급
        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )
        
        val issuedCoupon = couponService.issueCoupon(issueCommand)
        val userCouponId = issuedCoupon.userCouponId!!
        
        // 쿠폰 사용 명령 생성
        val useCommand = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(userCouponId),
            totalAmount = Money.of(20000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 2,
                    subTotal = Money.of(20000)
                )
            ),
            timestamp = now,
            context = OrderSagaContext(
                order = mockk<Order>(),
                timestamp = LocalDateTime.now()
            )
        )

        // when
        val result = couponService.use(useCommand).getOrThrow()

        // then
        result.discountInfo.size shouldBe 1
        
        val discountInfo = result.discountInfo.first()
        discountInfo.orderItemId shouldBe 1L
        discountInfo.amount.compareTo(Money.of(5000)) shouldBe 0
        discountInfo.sourceId shouldBe couponId
        discountInfo.sourceType shouldBe "COUPON"
        
        // 사용자 쿠폰 상태 변경 확인
        val usedCoupon = userCouponRepository.findById(userCouponId)?: throw IllegalStateException()
        usedCoupon.status shouldBe UserCouponStatus.USED
        usedCoupon.usedAt shouldNotBe null
    }

    @Test
    fun `✅사용자의 쿠폰 목록을 페이징하여 조회한다`() {
        // given
        val userId = 10L
        val now = LocalDateTime.now()
        val pageable = PageRequest.of(0, 3)
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        repeat(10) {
            val coupon = CouponTestFixture.coupon().build()
            val savedCoupon = couponRepository.save(coupon)
            couponService.issueCoupon(CouponCommand.Issue(userId = 10L, couponId = savedCoupon.id!!))
        }

        // when
        val result = couponService.retrieveLists(userId, pageable)

        // then
        // 반환된 쿠폰 수가 페이징 크기와 일치하는지 확인
        result.coupons.size shouldBe 3
        
        // 페이징 정보 확인
        result.pageResult.page shouldBe 0
        result.pageResult.size shouldBe 3
        result.pageResult.totalElements shouldBe 10
        result.pageResult.totalPages shouldBe 4
        
        // 반환된 쿠폰이 올바른 정보를 가지는지 확인
        result.coupons.forEach { userCouponData ->
            userCouponData.couponId shouldNotBe null
            userCouponData.couponName shouldNotBe null
            userCouponData.description shouldNotBe null
            userCouponData.status shouldBe UserCouponStatus.UNUSED.name
            userCouponData.expiredAt shouldBe now.plusDays(10)
        }
    }

    @Test
    fun `✅쿠폰이 없는 사용자는 빈 목록을 반환한다`() {
        // given
        val nonExistentUserId = 999L
        val pageable = PageRequest.of(0, 10)

        // when
        val result = couponService.retrieveLists(nonExistentUserId, pageable)

        // then
        result.coupons.size shouldBe 0
        result.pageResult.totalElements shouldBe 0
        result.pageResult.totalPages shouldBe 0
    }
}