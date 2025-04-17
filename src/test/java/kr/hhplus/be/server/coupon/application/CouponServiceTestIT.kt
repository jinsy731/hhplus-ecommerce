package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.common.ClockHolder
import kr.hhplus.be.server.common.exception.CouponTargetNotFoundException
import kr.hhplus.be.server.common.exception.InvalidCouponStatusException
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import kr.hhplus.be.server.coupon.domain.model.FixedAmountTotalDiscountType
import kr.hhplus.be.server.coupon.domain.model.MinOrderAmountCondition
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaUserCouponRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@SpringBootTestWithMySQLContainer
class CouponServiceTestIT {

    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var jpaCouponRepository: JpaCouponRepository

    @Autowired
    private lateinit var jpaUserCouponRepository: JpaUserCouponRepository

    @MockitoBean
    private lateinit var mockClockHolder: ClockHolder

    @Test
    @Transactional
    fun `✅쿠폰 발급을 하면 사용자 쿠폰이 생성된다`() {
        // given
        val userId = 1L
        val now = LocalDateTime.now()
        val coupon = Coupon(
            id = null,
            name = "테스트 쿠폰",
            description = "통합 테스트용 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "1000원 정액 할인",
                discountType = FixedAmountTotalDiscountType(BigDecimal(1000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(5000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = now.minusDays(1),
            endAt = now.plusDays(30),
            validDays = 7,
            createdAt = now,
            updatedAt = now
        )
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)
        val savedCoupon = jpaCouponRepository.save(coupon)
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
        result.expiredAt shouldBe now.plusDays(7)

        // DB에 저장된 사용자 쿠폰 확인
        val userCoupon = jpaUserCouponRepository.findById(result.userCouponId!!).orElseThrow()
        userCoupon.userId shouldBe userId
        userCoupon.coupon.id shouldBe couponId
        userCoupon.status shouldBe UserCouponStatus.UNUSED

        // 쿠폰의 발급 횟수가 증가했는지 확인
        val updatedCoupon = couponRepository.getById(couponId)
        updatedCoupon.issuedCount shouldBe 1
    }

    @Test
    @Transactional
    fun `✅쿠폰을 사용하면 할인 정보가 생성된다`() {
        // given
        val userId = 2L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 쿠폰 생성 및 저장
        val coupon = Coupon(
            id = null,
            name = "할인 쿠폰",
            description = "5000원 할인 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "5000원 정액 할인",
                discountType = FixedAmountTotalDiscountType(BigDecimal(5000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = now.minusDays(1),
            endAt = now.plusDays(30),
            validDays = 7,
            createdAt = now,
            updatedAt = now
        )
        
        val savedCoupon = jpaCouponRepository.save(coupon)
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
            totalAmount = BigDecimal(20000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 2,
                    subTotal = BigDecimal(20000)
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
        discountInfo.amount.compareTo(BigDecimal(5000)) shouldBe 0
        discountInfo.sourceId shouldBe couponId
        discountInfo.sourceType shouldBe "COUPON"
        
        // 사용자 쿠폰 상태 변경 확인
        val usedCoupon = jpaUserCouponRepository.findById(userCouponId).orElseThrow()
        usedCoupon.status shouldBe UserCouponStatus.USED
        usedCoupon.usedAt shouldNotBe null
    }

    @Test
    @Transactional
    fun `❌최소 주문 금액을 충족하지 않는 경우 쿠폰 적용에 실패한다`() {
        // given
        val userId = 3L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 최소 주문 금액이 10000원인 쿠폰 생성
        val coupon = Coupon(
            id = null,
            name = "할인 쿠폰",
            description = "최소 주문 금액 10000원 쿠폰",
            discountPolicy = DiscountPolicy(
                name = "2000원 정액 할인",
                discountType = FixedAmountTotalDiscountType(BigDecimal(2000)),
                discountCondition = MinOrderAmountCondition(BigDecimal(10000))
            ),
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = now.minusDays(1),
            endAt = now.plusDays(30),
            validDays = 7,
            createdAt = now,
            updatedAt = now
        )
        
        val savedCoupon = jpaCouponRepository.save(coupon)
        val couponId = savedCoupon.id!!
        
        // 사용자 쿠폰 발급
        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )
        
        val issuedCoupon = couponService.issueCoupon(issueCommand)
        val userCouponId = issuedCoupon.userCouponId!!
        
        // 최소 주문 금액(10000원)보다 적은 8000원 주문에 쿠폰 적용 시도
        val useCommand = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(userCouponId),
            totalAmount = BigDecimal(8000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = BigDecimal(8000)
                )
            ),
            timestamp = now
        )

        // when & then
        shouldThrowExactly<CouponTargetNotFoundException> {
            couponService.use(useCommand)
        }
        
        // 쿠폰 상태는 여전히 UNUSED 여야 함
        val unusedCoupon = jpaUserCouponRepository.findById(userCouponId).orElseThrow()
        unusedCoupon.status shouldBe UserCouponStatus.UNUSED
        unusedCoupon.usedAt shouldBe null
    }

    @Test
    @Transactional
    fun `❌이미 사용된 쿠폰으로는 할인을 적용할 수 없다`() {
        // given
        val userId = 4L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 쿠폰 생성 - Fixture 사용 및 저장
        val discountPolicy = DiscountPolicy(
            name = "5000원 정액 할인",
            discountType = FixedAmountTotalDiscountType(BigDecimal(5000)),
            discountCondition = MinOrderAmountCondition(BigDecimal(10000))
        )
        
        val coupon = Coupon(
            id = null,
            name = "할인 쿠폰",
            description = "5000원 할인 쿠폰",
            discountPolicy = discountPolicy,
            isActive = true,
            maxIssueLimit = 10,
            issuedCount = 0,
            startAt = now.minusDays(1),
            endAt = now.plusDays(30),
            validDays = 7,
            createdAt = now,
            updatedAt = now
        )
        
        val savedCoupon = jpaCouponRepository.save(coupon)
        val couponId = savedCoupon.id!!
        
        // 사용자 쿠폰 발급
        val issueCommand = CouponCommand.Issue(
            userId = userId,
            couponId = couponId
        )
        
        val issuedCoupon = couponService.issueCoupon(issueCommand)
        val userCouponId = issuedCoupon.userCouponId!!
        
        // 첫 번째 쿠폰 사용
        val useCommand = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(userCouponId),
            totalAmount = BigDecimal(15000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = BigDecimal(15000)
                )
            ),
            timestamp = now
        )
        
        couponService.use(useCommand)
        
        // 동일한 쿠폰으로 두 번째 사용 시도
        val secondUseCommand = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(userCouponId),
            totalAmount = BigDecimal(12000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 2L,
                    productId = 2L,
                    variantId = 2L,
                    quantity = 1,
                    subTotal = BigDecimal(12000)
                )
            ),
            timestamp = now
        )

        // when & then
        shouldThrowExactly<InvalidCouponStatusException> {
            couponService.use(secondUseCommand)
        }
    }
}