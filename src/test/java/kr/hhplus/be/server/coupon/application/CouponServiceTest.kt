package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.common.ClockHolder
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.toUseCouponCommandItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponServiceTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var discountLineRepository: DiscountLineRepository
    private lateinit var couponService: CouponService
    private lateinit var clockHolder: ClockHolder

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        discountLineRepository = mockk()
        clockHolder = mockk()
        couponService = CouponService(couponRepository, userCouponRepository, discountLineRepository, clockHolder)
    }
    
    @Test
    fun `✅쿠폰적용`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val discountAmount = BigDecimal(1000)
        val coupon = CouponTestFixture.createValidCoupon(
            id = 1L,
            discountPolicy = DiscountPolicy(
                name = "",
                discountType = FixedAmountTotalDiscountType(discountAmount),
                discountCondition = AllProductCondition()
            )
        )

        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )

        val discountLine = listOf(DiscountLine(
            orderItemId = 1L,
            amount = discountAmount,
            sourceId = 1L,
            type = DiscountMethod.COUPON,
        ))

        // 모든 주문 상품이 할인 조건을 만족한다고 가정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        every { discountLineRepository.saveAll(any()) } returns discountLine
        every { clockHolder.getNowInLocalDateTime() } returns now


        val cmd = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = BigDecimal(1000),
            items = listOf(CouponCommand.Use.Item(
                orderItemId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 1,
                subTotal = BigDecimal(1000)
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
        val coupon = CouponTestFixture.createValidCoupon()

        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )


        val order = OrderTestFixture.createOrder(userId).apply {
            this.originalTotal = BigDecimal(5000) // 할인 기준: 10000원 이상 구매
        }

        // 모든 주문 상품이 할인 조건을 만족한다고 가정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        every { userCouponRepository.saveAll(any()) } returns listOf(userCoupon)
        every { discountLineRepository.saveAll(any()) } returns listOf(mockk())

        val cmd = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(1L),
            totalAmount = order.originalTotal,
            items = order.orderItems.toUseCouponCommandItem(),
            timestamp = now
        )
        // act
        shouldThrowAny { couponService.use(cmd) }

        // assert
        verify(exactly = 0) { discountLineRepository.saveAll(any()) }
    }


    @Test
    fun `✅쿠폰 발급`() {
        // arrange
        val now = LocalDateTime.now()
        val coupon = CouponTestFixture.createValidCoupon()
        val userCoupon = coupon.issueTo(1L)
        val cmd = CouponCommand.Issue(1L, 1L)
        every { couponRepository.getById(1L) } returns coupon
        every { userCouponRepository.save(any()) } returns userCoupon
        every { clockHolder.getNowInLocalDateTime() } returns now
        // act
        val result = couponService.issueCoupon(cmd)
        // assert
        result.status shouldBe UserCouponStatus.UNUSED
        verify(exactly = 1) { couponRepository.getById(1L) }
        verify(exactly = 1) { userCouponRepository.save(any()) }
    }
}
