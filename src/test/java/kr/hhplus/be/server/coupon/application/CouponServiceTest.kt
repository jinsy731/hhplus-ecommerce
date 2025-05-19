package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.shared.time.ClockHolder
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.DuplicateCouponIssueException
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.CouponKVStore
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.toUseCouponCommandItem
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

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        discountLineRepository = mockk()
        clockHolder = mockk()
        couponKVStore = mockk()
        couponService = CouponService(couponRepository, userCouponRepository, discountLineRepository, couponKVStore, clockHolder)
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
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = Money.of(1000),
            items = listOf(CouponCommand.Use.Item(
                orderItemId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 1,
                subTotal = Money.of(1000)
            )),
            timestamp = now
        )
        
        // act
        val result = couponService.use(cmd)
        
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
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = order.originalTotal, // 5000원 (쿠폰 적용 조건인 10000원 미만)
            items = order.orderItems.toUseCouponCommandItem(),
            timestamp = now
        )
        
        // act & assert
        shouldThrowAny { couponService.use(cmd) }

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
}
