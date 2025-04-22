package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.common.ClockHolder
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.CouponTargetNotFoundException
import kr.hhplus.be.server.common.exception.InvalidCouponStatusException
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.CouponTestFixture.coupon
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaUserCouponRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class CouponServiceTestIT @Autowired constructor(
    private val couponService: CouponService,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    @MockitoBean private val mockClockHolder: ClockHolder,
    private val databaseCleaner: MySqlDatabaseCleaner,
){

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
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
            timestamp = now
        )

        // when
        val result = couponService.use(useCommand)

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