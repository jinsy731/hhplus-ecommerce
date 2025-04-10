package kr.hhplus.be.server.coupon.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import kr.hhplus.be.server.order.domain.model.OrderItemStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponServiceTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        couponService = CouponService(userCouponRepository)
    }
    
    @Test
    fun `✅쿠폰적용_쿠폰이 적용될 대상이 있으면 상태가 USED로 변경된다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val coupon = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))

        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )

        val order = OrderTestFixture.createOrder(userId).apply {
            this.originalTotal = BigDecimal(20000)
        }

        // 모든 주문 상품이 할인 조건을 만족한다고 가정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)

        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(1L),
            now = now
        )
        // act
        couponService.applyCoupon(cmd)
        
        // assert
        userCoupon.status shouldBe UserCouponStatus.USED
    }

    @Test
    fun `⛔️쿠폰적용_쿠폰이 적용될 대상이 없으면 상태가 변경되지 않는다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val coupon = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))

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

        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(1L),
            now = now
        )
        // act
        couponService.applyCoupon(cmd)

        // assert
        userCoupon.status shouldBe UserCouponStatus.UNUSED
    }

    @Test
    fun `✅ 쿠폰적용_쿠폰을 주문에 적용하면 할인이 계산된다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val coupon = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))
        
        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )
        
        val order = OrderTestFixture.createOrder(userId).apply {
            this.originalTotal = BigDecimal(20000)
        }
        
        // 모든 주문 상품이 할인 조건을 만족한다고 가정
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        
        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(1L),
            now = now
        )

        // act
        val result = couponService.applyCoupon(cmd)

        // assert
        result.totalDiscount.compareTo(BigDecimal(5000)) shouldBe 0  // FixedAmountDiscountType가 5000원 할인
        result.discountPerItem.size shouldBe 2
        
        // 각 아이템에 대한 할인이 제대로 할당됐는지 확인
        val halfDiscount = BigDecimal(2500)
        result.discountPerItem[0].discountAmount.compareTo(halfDiscount) shouldBe 0
        result.discountPerItem[1].discountAmount.compareTo(halfDiscount) shouldBe 0
        
        // UserCoupon 상태가 USED로 변경되었는지 간접 확인
        verify(exactly = 1) { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) }
    }

    @Test
    fun `✅ 쿠폰적용_여러 개의 쿠폰을 주문에 적용하면 총 할인이 계산된다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        
        val coupon1 = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))
        val coupon2 = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))
        
        val userCoupon1 = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon1,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )
        
        val userCoupon2 = UserCoupon(
            id = 2L,
            userId = userId,
            coupon = coupon2,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )
        
        val order = OrderTestFixture.createOrder(userId).apply {
            this.originalTotal = BigDecimal(20000)
        }
        
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L, 2L)) } returns listOf(userCoupon1, userCoupon2)
        
        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(1L, 2L),
            now = now
        )

        // act
        val result = couponService.applyCoupon(cmd)

        // assert
        result.totalDiscount.compareTo(BigDecimal(10000)) shouldBe 0  // 두 개의 5000원 정액 할인 쿠폰 적용
        
        // 두 개의 쿠폰이 적용되므로 각 아이템에 할인이 2번 적용됨 (총 4개의 할인 항목)
        result.discountPerItem.size shouldBe 4
        
        verify(exactly = 1) { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L, 2L)) }
    }

    @Test
    fun `✅ 쿠폰적용_할인 금액이 여러 아이템에 비례하여 분배된다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        val coupon = CouponTestFixture.createValidCoupon(startAt = now.minusDays(1), endAt = now.plusDays(1))
        
        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            coupon = coupon,
            issuedAt = now.minusHours(5),
            expiredAt = now.plusDays(3),
            status = UserCouponStatus.UNUSED
        )
        
        // 다른 가격의 상품 2개를 포함하는 주문
        val item1 = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(15000),
            status = OrderItemStatus.ORDERED
        )
        
        val item2 = OrderItem(
            id = 2L,
            productId = 2L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(5000),
            status = OrderItemStatus.ORDERED
        )
        
        val order = Order(
            id = 1L,
            userId = userId,
            orderItems = mutableListOf(item1, item2),
            originalTotal = BigDecimal(20000)
        )
        
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) } returns listOf(userCoupon)
        
        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(1L),
            now = now
        )

        // act
        val result = couponService.applyCoupon(cmd)

        // assert
        result.totalDiscount.compareTo(BigDecimal(5000)) shouldBe 0  // 5000원 정액 할인 적용
        result.discountPerItem.size shouldBe 2
        
        // 할인이 금액 비율대로 분배되어야 함
        // item1: 15000원 (75%), item2: 5000원 (25%)
        val discount1 = result.discountPerItem.find { it.orderItemId == 1L }!!.discountAmount
        val discount2 = result.discountPerItem.find { it.orderItemId == 2L }!!.discountAmount
        
        // 할인액의 비율 검증 (대략 75:25 비율로 3750:1250)
        discount1.compareTo(BigDecimal(3750)) shouldBe 0
        discount2.compareTo(BigDecimal(1250)) shouldBe 0
        
        verify(exactly = 1) { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf(1L)) }
    }

    @Test
    fun `✅ 쿠폰적용_유효한 쿠폰이 없으면 할인이 적용되지 않는다`() {
        // arrange
        val userId = 1L
        val now = LocalDateTime.now()
        
        val order = OrderTestFixture.createOrder(userId).apply {
            this.originalTotal = BigDecimal(20000)
        }
        
        every { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf()) } returns emptyList()
        
        val cmd = CouponCommand.ApplyToOrder(
            userId = userId,
            order = order,
            userCouponIds = listOf(),
            now = now
        )

        // act
        val result = couponService.applyCoupon(cmd)

        // assert
        result.totalDiscount shouldBe BigDecimal.ZERO
        result.discountPerItem.size shouldBe 0
        
        verify(exactly = 1) { userCouponRepository.findAllByUserIdAndIdIsIn(userId, listOf()) }
    }
    
    @Test
    fun `✅ 쿠폰적용_distributeDiscount 메소드는 할인액을 정확히 분배한다`() {
        // arrange
        val item1 = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(6000),
            status = OrderItemStatus.ORDERED
        )
        
        val item2 = OrderItem(
            id = 2L,
            productId = 2L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(4000),
            status = OrderItemStatus.ORDERED
        )
        
        val totalDiscount = BigDecimal(1000)
        
        // act
        val result = couponService.distributeDiscount(listOf(item1, item2), totalDiscount)
        
        // assert
        result.size shouldBe 2
        
        // 금액 비율대로 분배 (6:4)
        val discount1 = result.find { it.orderItemId == 1L }!!.discountAmount
        val discount2 = result.find { it.orderItemId == 2L }!!.discountAmount
        
        discount1.compareTo(BigDecimal(600)) shouldBe 0
        discount2.compareTo(BigDecimal(400)) shouldBe 0
        
        // 할인액 합계가 총 할인액과 일치
        result.sumOf { it.discountAmount }.compareTo(totalDiscount) shouldBe 0
    }
    
    @Test
    fun `✅ 쿠폰적용_마지막 아이템은 남은 할인 잔액을 모두 받는다`() {
        // arrange
        val item1 = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(3000),
            status = OrderItemStatus.ORDERED
        )
        
        val item2 = OrderItem(
            id = 2L,
            productId = 2L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(3000),
            status = OrderItemStatus.ORDERED
        )
        
        val item3 = OrderItem(
            id = 3L,
            productId = 3L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(4000),
            status = OrderItemStatus.ORDERED
        )
        
        // 10000원 중 1000원 할인
        val totalDiscount = BigDecimal(1000)
        
        // act
        val result = couponService.distributeDiscount(listOf(item1, item2, item3), totalDiscount)
        
        // assert
        result.size shouldBe 3
        
        // 할인액 합계가 총 할인액과 일치하는지 확인
        result.sumOf { it.discountAmount }.compareTo(totalDiscount) shouldBe 0
        
        // 마지막 아이템은 남은 할인 잔액을 모두 받아야 함
        // (반올림 오차가 마지막 아이템에 모두 반영됨)
        val discount3 = result.find { it.orderItemId == 3L }!!.discountAmount
        val expected3 = BigDecimal(400)
        discount3.compareTo(expected3) shouldBe 0
    }
}
