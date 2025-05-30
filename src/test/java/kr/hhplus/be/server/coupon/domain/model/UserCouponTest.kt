package kr.hhplus.be.server.coupon.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.ErrorCode
import kr.hhplus.be.server.shared.exception.ExpiredCouponException
import kr.hhplus.be.server.shared.exception.InvalidCouponStatusException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class UserCouponTest {

    @Test
    fun `✅유저 쿠폰 생성`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val couponId = 1L

        // act
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
        )

        // assert
        userCoupon.id shouldBe 1L
        userCoupon.userId shouldBe 1L
        userCoupon.couponId shouldBe couponId
        userCoupon.expiredAt shouldBe expiredAt
        userCoupon.status shouldBe UserCouponStatus.UNUSED
        userCoupon.issuedAt shouldBe time
    }

    @Test
    fun `✅유저 쿠폰 사용_사용 가능한 상태이고 유효기간이 지나지 않았다면 쿠폰이 사용되어 상태가 USED, 사용시간이 변경되어야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val orderId = 1L
        val couponId = 1L
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
        )

        // act
        userCoupon.use(time, orderId)

        // assert
        userCoupon.status shouldBe UserCouponStatus.USED
        userCoupon.usedAt shouldBe time
        userCoupon.orderId shouldBe orderId
    }
    
    @ParameterizedTest
    @ValueSource(strings = ["USED", "EXPIRED"])
    fun `⛔️유저 쿠폰 사용 실패_사용 가능한 상태(USED, EXPIRED) 이면 InvalidCouponStatus 예외가 발생해야 한다`(status: UserCouponStatus) {
        // arrange
        val orderId = 1L
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val couponId = 1L
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
            status = status
        )

        // act, assert
        val ex = shouldThrowExactly<InvalidCouponStatusException> { userCoupon.use(time, orderId) }
        ex.message shouldBe ErrorCode.INVALID_COUPON_STATUS.message
    }
    
    @Test
    fun `⛔️유저 쿠폰 사용 실패_유효기간이 지나면 status가 EXPIRED로 변경되고 ExpiredCouponException 예외가 발생해야 한다`() {
        // arrange
        val orderId = 1L
        val time = LocalDateTime.now()
        val expiredAt = time.minusMinutes(1)
        val couponId = 1L
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
            status = UserCouponStatus.UNUSED
        )
        // act, assert
        val ex = shouldThrowExactly<ExpiredCouponException> { userCoupon.use(time, orderId) }
        ex.message shouldBe ErrorCode.EXPIRED_COUPON.message
        userCoupon.status shouldBe UserCouponStatus.EXPIRED
    }

    @Test
    fun `✅쿠폰 복구_사용된 쿠폰을 복구하면 UNUSED 상태로 변경된다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val orderId = 1L
        val couponId = 1L
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
            status = UserCouponStatus.USED,
            usedAt = time,
            orderId = orderId
        )

        // act
        userCoupon.restore()

        // assert
        userCoupon.status shouldBe UserCouponStatus.UNUSED
        userCoupon.usedAt shouldBe null
        userCoupon.orderId shouldBe null
    }

    @Test
    fun `✅도메인 서비스를 통한 할인 계산_도메인 서비스에 위임하여 처리한다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val orderId = 1L
        val couponId = 1L
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            issuedAt = time,
            expiredAt = expiredAt,
            status = UserCouponStatus.UNUSED
        )

        val mockDomainService = mockk<CouponDomainService>()
        val mockContext = mockk<DiscountContext.Root>()
        val expectedDiscountLines = listOf(
            DiscountLine(
                orderItemId = 1L,
                amount = Money.of(1000),
                sourceId = 1L,
                type = DiscountMethod.COUPON
            )
        )

        every { 
            mockDomainService.calculateDiscountAndUse(userCoupon, mockContext, orderId) 
        } returns expectedDiscountLines

        // act
        val result = userCoupon.calculateDiscountAndUse(mockDomainService, mockContext, orderId)

        // assert
        result shouldBe expectedDiscountLines
        verify(exactly = 1) { 
            mockDomainService.calculateDiscountAndUse(userCoupon, mockContext, orderId) 
        }
    }
}