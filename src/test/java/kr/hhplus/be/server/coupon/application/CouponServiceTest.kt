package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.application.mapper.CouponMapper
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponEventPublisher
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponIssueValidationResult
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.CouponOutOfStockException
import kr.hhplus.be.server.shared.exception.DuplicateCouponIssueException
import kr.hhplus.be.server.shared.time.ClockHolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CouponServiceTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var discountLineRepository: DiscountLineRepository
    private lateinit var couponService: CouponService
    private lateinit var clockHolder: ClockHolder
    private lateinit var couponKVStore: CouponKVStore
    private lateinit var eventPublisher: CouponEventPublisher
    private lateinit var couponDomainService: CouponDomainService
    private val couponMapper = CouponMapper()

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        discountLineRepository = mockk()
        clockHolder = mockk()
        couponKVStore = mockk()
        eventPublisher = mockk()
        couponDomainService = mockk()

        // 기본 mock 설정
        every { couponKVStore.setIssuedStatus(any(), any(), any()) } just Runs
        every { couponKVStore.rollbackCouponIssue(any(), any()) } returns true
        every { eventPublisher.publishIssueRequested(any()) } just Runs

        couponService = CouponService(
            couponRepository = couponRepository,
            userCouponRepository = userCouponRepository,
            discountLineRepository = discountLineRepository,
            couponKVStore = couponKVStore,
            clockHolder = clockHolder,
            couponMapper = couponMapper,
            couponEventPublisher = eventPublisher,
            couponDomainService = couponDomainService,
        )
    }
    
    @Test
    fun `✅쿠폰적용`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val discountAmount = Money.of(1000)
        
        // 모든 상품에 적용 가능한 1000원 할인 쿠폰 생성
        val coupon = CouponTestFixture.coupon(id = 1L)
            .withDiscountPolicy(
                DiscountPolicy(
                    name = "1000원 할인",
                    discountType = FixedAmountTotalDiscountType(discountAmount),
                    discountCondition = AllProductCondition()
                )
            )
            .build()

        // 사용자 쿠폰 생성
        val userCoupon = CouponTestFixture.userCoupon(
            id = 1L,
            userId = userId,
            couponId = 1L
        ).build()

        // 할인 내역 생성
        val discountLine = listOf(DiscountLine(
            orderItemId = 1L,
            amount = discountAmount,
            sourceId = 1L,
            type = DiscountMethod.COUPON,
        ))

        // Mock 설정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        every { couponDomainService.calculateDiscountAndUse(any(), any(), any()) } returns discountLine
        every { discountLineRepository.saveAll(any()) } returns discountLine
        every { clockHolder.getNowInLocalDateTime() } returns now

        // 쿠폰 사용 명령 생성
        val cmd = CouponCommand.Use.Root(
            orderId = 1L,
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = Money.of(1000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(1000)
                )
            ),
            timestamp = now,
        )
        
        // act
        val result = couponService.use(cmd).getOrThrow()
        
        // assert
        result.discountInfo.first()
        verify(exactly = 1) { discountLineRepository.saveAll(any()) }
        verify(exactly = 1) { couponDomainService.calculateDiscountAndUse(any(), any(), any()) }
    }

    @Test
    fun `⛔️쿠폰적용_쿠폰 적용이 실패하면 DiscountLine이 저장되지 않는다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        
        // 사용자 쿠폰 생성
        val userCoupon = CouponTestFixture.userCoupon(
            id = 1L,
            userId = userId,
            couponId = 1L
        ).build()

        // 금액이 적은 주문 생성 (5000원, 10000원 미만으로 쿠폰 적용 조건 미충족)
        val order = OrderTestFixture.lowAmountOrder(userId)

        // Mock 설정 - 도메인 서비스에서 예외 발생
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        every { couponDomainService.calculateDiscountAndUse(any(), any(), any()) } throws RuntimeException("쿠폰 적용 조건 미충족")

        // 쿠폰 사용 명령 생성
        val cmd = CouponCommand.Use.Root(
            orderId = 1L,
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = order.originalTotal, // 5000원 (쿠폰 적용 조건인 10000원 미만)
            items = listOf(CouponCommand.Use.Item(
                orderItemId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 1,
                subTotal = order.originalTotal
            )),
            timestamp = now,
        )
        
        // act & assert
        val result = couponService.use(cmd)
        result.isFailure shouldBe true

        // 할인 내역이 저장되지 않았는지 검증
        verify(exactly = 0) { discountLineRepository.saveAll(any()) }
    }

    @Test
    fun `✅쿠폰 발급`() {
        // arrange
        val now = LocalDateTime.now()
        
        // 유효한 쿠폰 생성
        val coupon = CouponTestFixture.validFixedAmountCoupon(id = 1L)
        
        // 사용자에게 발급될 쿠폰
        val userCoupon = coupon.issueTo(1L)
        
        // 쿠폰 발급 명령 생성
        val cmd = CouponCommand.Issue(userId = 1L, couponId = 1L)
        
        // Mock 설정
        every { couponRepository.getByIdForUpdate(1L) } returns coupon
        every { userCouponRepository.save(any()) } returns userCoupon
        every { clockHolder.getNowInLocalDateTime() } returns now
        every { userCouponRepository.findByUserIdAndCouponId(any(), any())} returns null
        
        // act
        val result = couponService.issueCoupon(cmd)
        
        // assert
        result.status shouldBe UserCouponStatus.UNUSED
        verify(exactly = 1) { couponRepository.getByIdForUpdate(1L) }
        verify(exactly = 1) { userCouponRepository.save(any()) }
    }
    @Test
    fun `❌쿠폰 발급 실패_이미 발급된 쿠폰인경우 DuplicateCouponIssueException을 발생시켜야 한다`() {
        // arrange
        val now = LocalDateTime.now()

        // 유효한 쿠폰 생성
        val coupon = CouponTestFixture.validFixedAmountCoupon(id = 1L)

        // 사용자에게 발급될 쿠폰
        val userCoupon = coupon.issueTo(1L)

        // 쿠폰 발급 명령 생성
        val cmd = CouponCommand.Issue(userId = 1L, couponId = 1L)

        // Mock 설정
        every { userCouponRepository.findByUserIdAndCouponId(1L, 1L)} returns userCoupon
        every { couponRepository.getById(1L) } returns coupon
        every { userCouponRepository.save(any()) } returns userCoupon
        every { clockHolder.getNowInLocalDateTime() } returns now

        // act
        shouldThrowExactly<DuplicateCouponIssueException> { couponService.issueCoupon(cmd) }

        // assert
        verify(exactly = 0) { couponRepository.getById(any()) }
        verify(exactly = 0) { userCouponRepository.save(any()) }
    }

    @Test
    fun `✅쿠폰 복구가 정상적으로 동작한다`() {
        // given
        val userId = 1L
        val orderId = 1L
        val userCouponIds = listOf(1L, 2L)
        val couponId = 1L
        val userCoupons = listOf(
            CouponTestFixture.userCoupon(userId = userId, couponId = couponId, status = UserCouponStatus.USED, orderId = orderId).build(),
            CouponTestFixture.userCoupon(userId = userId, couponId = couponId, status = UserCouponStatus.USED, orderId = orderId).build()
        )

        every { userCouponRepository.findAllByOrderId(orderId) } returns userCoupons
        every { userCouponRepository.saveAll(userCoupons) } returns userCoupons

        // when
        couponService.restoreCoupons(
            orderId = userId,
            orderEventPayload = OrderEventPayload(
                orderId = 1L,
                userId = userId,
                originalTotal = Money.ZERO,
                discountedAmount = Money.ZERO,
                orderItems = emptyList(),
                timestamp = LocalDateTime.now(),
                userCouponIds = userCouponIds,
            ),
        )

        // then
        userCoupons.forEach { userCoupon ->
            userCoupon.status shouldBe UserCouponStatus.UNUSED
            userCoupon.usedAt shouldBe null
        }
        verify { userCouponRepository.saveAll(userCoupons) }
    }

    @Test
    fun `✅Redis Kafka 쿠폰 발급이 성공한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val now = LocalDateTime.now()
        val cmd = CouponCommand.Issue(userId = userId, couponId = couponId)
        
        val validationResult = CouponIssueValidationResult(isValid = true)
        
        every { couponKVStore.validateAndMarkCouponIssue(userId, couponId) } returns validationResult
        every { clockHolder.getNowInLocalDateTime() } returns now

        // when
        val result = couponService.issueCouponWithRedisKafka(cmd)

        // then
        result.couponId shouldBe couponId
        result.status shouldBe "REQUESTED"
        
        verify { couponKVStore.validateAndMarkCouponIssue(userId, couponId) }
        verify { couponKVStore.setIssuedStatus(userId, couponId, IssuedStatus.PENDING) }
        verify { eventPublisher.publishIssueRequested(any()) }
    }

    @Test
    fun `⛔️Redis Kafka 쿠폰 발급 시 중복 발급으로 실패한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val cmd = CouponCommand.Issue(userId = userId, couponId = couponId)
        
        val validationResult = CouponIssueValidationResult(
            isValid = false,
            errorCode = CouponIssueValidationResult.ERROR_DUPLICATE_ISSUE,
            errorMessage = "이미 발급된 쿠폰입니다"
        )
        
        every { couponKVStore.validateAndMarkCouponIssue(userId, couponId) } returns validationResult

        // when & then
        shouldThrowExactly<DuplicateCouponIssueException> {
            couponService.issueCouponWithRedisKafka(cmd)
        }
        
        verify { couponKVStore.validateAndMarkCouponIssue(userId, couponId) }
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
        verify(exactly = 0) { eventPublisher.publishIssueRequested(any()) }
    }

    @Test
    fun `⛔️Redis Kafka 쿠폰 발급 시 재고 부족으로 실패한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val cmd = CouponCommand.Issue(userId = userId, couponId = couponId)
        
        val validationResult = CouponIssueValidationResult(
            isValid = false,
            errorCode = CouponIssueValidationResult.ERROR_OUT_OF_STOCK,
            errorMessage = "쿠폰 재고가 부족합니다"
        )
        
        every { couponKVStore.validateAndMarkCouponIssue(userId, couponId) } returns validationResult

        // when & then
        shouldThrowExactly<CouponOutOfStockException> {
            couponService.issueCouponWithRedisKafka(cmd)
        }
        
        verify { couponKVStore.validateAndMarkCouponIssue(userId, couponId) }
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
        verify(exactly = 0) { eventPublisher.publishIssueRequested(any()) }
    }

    @Test
    fun `⛔️Redis Kafka 쿠폰 발급 시 이벤트 발행 실패하면 보상 처리를 수행한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val now = LocalDateTime.now()
        val cmd = CouponCommand.Issue(userId = userId, couponId = couponId)
        
        val validationResult = CouponIssueValidationResult(isValid = true)
        
        every { couponKVStore.validateAndMarkCouponIssue(userId, couponId) } returns validationResult
        every { clockHolder.getNowInLocalDateTime() } returns now
        every { eventPublisher.publishIssueRequested(any()) } throws RuntimeException("이벤트 발행 실패")

        // when & then
        shouldThrowExactly<RuntimeException> {
            couponService.issueCouponWithRedisKafka(cmd)
        }
        
        verify { couponKVStore.validateAndMarkCouponIssue(userId, couponId) }
        verify { couponKVStore.setIssuedStatus(userId, couponId, IssuedStatus.PENDING) }
        verify { eventPublisher.publishIssueRequested(any()) }
        verify { couponKVStore.rollbackCouponIssue(userId, couponId) }
    }
}
