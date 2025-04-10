package kr.hhplus.be.server.coupon.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kr.hhplus.be.server.common.ErrorCode
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.ExpiredCouponException
import kr.hhplus.be.server.coupon.domain.InvalidCouponStatusException
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
        val coupon = CouponTestFixture.createValidCoupon()

        // act
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            coupon = coupon,
            issuedAt = time,
            expiredAt = expiredAt,
        )

        // assert
        userCoupon.id shouldBe 1L
        userCoupon.userId shouldBe 1L
        userCoupon.coupon shouldBeSameInstanceAs coupon
        userCoupon.expiredAt shouldBe expiredAt
        userCoupon.status shouldBe UserCouponStatus.UNUSED
        userCoupon.issuedAt shouldBe time
    }

    @Test
    fun `✅유저 쿠폰 사용_사용 가능한 상태이고 유효기간이 지나지 않았다면 쿠폰이 사용되어 상태가 USED, 사용시간이 변경되어야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val coupon = CouponTestFixture.createValidCoupon()
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            coupon = coupon,
            issuedAt = time,
            expiredAt = expiredAt,
        )

        // act
        userCoupon.use(time)

        // assert
        userCoupon.status shouldBe UserCouponStatus.USED
        userCoupon.usedAt shouldBe time
    }
    
    @ParameterizedTest
    @ValueSource(strings = ["USED", "EXPIRED"])
    fun `⛔️유저 쿠폰 사용 실패_사용 가능한 상태(USED, EXPIRED) 이면 InvalidCouponStatus 예외가 발생해야 한다`(status: UserCouponStatus) {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusHours(1)
        val coupon = CouponTestFixture.createValidCoupon()
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            coupon = coupon,
            issuedAt = time,
            expiredAt = expiredAt,
            status = status
        )

        // act, assert
        val ex = shouldThrowExactly<InvalidCouponStatusException> { userCoupon.use(time) }
        ex.message shouldBe ErrorCode.INVALID_COUPON_STATUS.message
    }
    
    @Test
    fun `⛔️유저 쿠폰 사용 실패_유효기간이 지나면 status가 EXPIRED로 변경되고 ExpiredCouponException 예외가 발생해야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.minusMinutes(1)
        val coupon = CouponTestFixture.createValidCoupon()
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            coupon = coupon,
            issuedAt = time,
            expiredAt = expiredAt,
            status = UserCouponStatus.UNUSED
        )
        // act, assert
        val ex = shouldThrowExactly<ExpiredCouponException> { userCoupon.use(time) }
        ex.message shouldBe ErrorCode.EXPIRED_COUPON.message
        userCoupon.status shouldBe UserCouponStatus.EXPIRED
    }
    
    @Test
    fun `⛔️유저 쿠폰 사용 실패_쿠폰이 유효하지 않으면(coupon#isValid == false) InvalidCouponStatusException 예외가 발생해야 한다`() {
        // arrange
        val time = LocalDateTime.now()
        val expiredAt = time.plusMinutes(1)
        val coupon = CouponTestFixture.createInvalidCoupon()
        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            coupon = coupon,
            issuedAt = time,
            expiredAt = expiredAt,
            status = UserCouponStatus.UNUSED
        )
        // act, assert
        val ex = shouldThrowExactly<InvalidCouponStatusException> { userCoupon.use(time) }
        ex.message shouldBe ErrorCode.INVALID_COUPON_STATUS.message
    }
}