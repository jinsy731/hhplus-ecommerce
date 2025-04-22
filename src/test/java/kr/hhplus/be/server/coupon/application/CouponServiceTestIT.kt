package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.common.ClockHolder
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.CouponTargetNotFoundException
import kr.hhplus.be.server.common.exception.InvalidCouponStatusException
import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
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

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
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
                discountType = FixedAmountTotalDiscountType(Money.of(1000)),
                discountCondition = MinOrderAmountCondition(Money.of(5000))
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
                discountType = FixedAmountTotalDiscountType(Money.of(5000)),
                discountCondition = MinOrderAmountCondition(Money.of(10000))
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
        val usedCoupon = jpaUserCouponRepository.findById(userCouponId).orElseThrow()
        usedCoupon.status shouldBe UserCouponStatus.USED
        usedCoupon.usedAt shouldNotBe null
    }

    @Test
    fun `✅사용자의 쿠폰 목록을 페이징하여 조회한다`() {
        // given
        val userId = 10L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 여러 개의 쿠폰 생성 및 저장
        val coupons = mutableListOf<Coupon>()
        for (i in 1..5) {
            val coupon = Coupon(
                id = null,
                name = "테스트 쿠폰 $i",
                description = "통합 테스트용 쿠폰 $i",
                discountPolicy = DiscountPolicy(
                    name = "${i}00원 정액 할인",
                    discountType = FixedAmountTotalDiscountType(Money.of(i * 1000L)),
                    discountCondition = MinOrderAmountCondition(Money.of(5000))
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
            coupons.add(jpaCouponRepository.save(coupon))
        }
        
        // 사용자에게 모든 쿠폰 발급
        coupons.forEach { coupon ->
            couponService.issueCoupon(
                CouponCommand.Issue(
                    userId = userId,
                    couponId = coupon.id!!
                )
            )
        }
        
        // 페이징 정보 설정 (1페이지, 3개 항목)
        val pageable = PageRequest.of(0, 3)

        // when
        val result = couponService.retrieveLists(userId, pageable)

        // then
        // 반환된 쿠폰 수가 페이징 크기와 일치하는지 확인
        result.coupons.size shouldBe 3
        
        // 페이징 정보 확인
        result.pageResult.page shouldBe 0
        result.pageResult.size shouldBe 3
        result.pageResult.totalElements shouldBe 5
        result.pageResult.totalPages shouldBe 2
        
        // 반환된 쿠폰이 올바른 정보를 가지는지 확인
        result.coupons.forEach { userCouponData ->
            userCouponData.couponId shouldNotBe null
            userCouponData.couponName shouldNotBe null
            userCouponData.description shouldNotBe null
            userCouponData.status shouldBe UserCouponStatus.UNUSED.name
            userCouponData.expiredAt shouldBe now.plusDays(7)
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

    @Test
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
                discountType = FixedAmountTotalDiscountType(Money.of(2000)),
                discountCondition = MinOrderAmountCondition(Money.of(10000))
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
            totalAmount = Money.of(8000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(8000)
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
    fun `❌이미 사용된 쿠폰으로는 할인을 적용할 수 없다`() {
        // given
        val userId = 4L
        val now = LocalDateTime.now()
        whenever(mockClockHolder.getNowInLocalDateTime()).thenReturn(now)

        // 쿠폰 생성 - Fixture 사용 및 저장
        val discountPolicy = DiscountPolicy(
            name = "5000원 정액 할인",
            discountType = FixedAmountTotalDiscountType(Money.of(5000)),
            discountCondition = MinOrderAmountCondition(Money.of(10000))
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
            totalAmount = Money.of(15000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    subTotal = Money.of(15000)
                )
            ),
            timestamp = now
        )
        
        couponService.use(useCommand)
        
        // 동일한 쿠폰으로 두 번째 사용 시도
        val secondUseCommand = CouponCommand.Use.Root(
            userId = userId,
            userCouponIds = listOf(userCouponId),
            totalAmount = Money.of(12000),
            items = listOf(
                CouponCommand.Use.Item(
                    orderItemId = 2L,
                    productId = 2L,
                    variantId = 2L,
                    quantity = 1,
                    subTotal = Money.of(12000)
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