package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.application.mapper.CouponMapper
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.shared.domain.DomainEvent
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
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
    private lateinit var eventPublisher: DomainEventPublisher
    private val couponMapper = CouponMapper()

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        discountLineRepository = mockk()
        clockHolder = mockk()
        couponKVStore = mockk()
        eventPublisher = mockk()

        every { eventPublisher.publish(any<DomainEvent<*>>()) } returns Unit
        couponService = CouponService(
            couponRepository = couponRepository,
            userCouponRepository = userCouponRepository,
            discountLineRepository = discountLineRepository,
            couponKVStore = couponKVStore,
            clockHolder = clockHolder,
            eventPublisher = eventPublisher,
            couponMapper = couponMapper
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
            coupon = coupon
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
    }

    @Test
    fun `⛔️쿠폰적용_쿠폰 적용이 실패하면 DiscountLine이 저장되지 않는다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        
        // 10000원 이상 구매 시 적용되는 5000원 할인 쿠폰 생성
        val coupon = CouponTestFixture.validFixedAmountCoupon()
        
        // 미사용 상태의 사용자 쿠폰 생성
        val userCoupon = CouponTestFixture.userCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon
        ).build()

        // 금액이 적은 주문 생성 (5000원, 10000원 미만으로 쿠폰 적용 조건 미충족)
        val order = OrderTestFixture.lowAmountOrder(userId)

        // Mock 설정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        every { userCouponRepository.saveAll(any()) } returns listOf(userCoupon)
        every { discountLineRepository.saveAll(any()) } returns listOf(mockk())

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
        shouldNotThrowAny { couponService.use(cmd) }

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
        val coupon = CouponTestFixture.coupon().build()
        val userCoupons = listOf(
            CouponTestFixture.userCoupon(userId = userId, coupon = coupon, status = UserCouponStatus.USED, orderId = orderId).build(),
            CouponTestFixture.userCoupon(userId = userId, coupon = coupon, status = UserCouponStatus.USED, orderId = orderId).build()
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
}
