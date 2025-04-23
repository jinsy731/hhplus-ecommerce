package kr.hhplus.be.server.coupon.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaUserCouponRepository
import kr.hhplus.be.server.executeConcurrently
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.optionals.getOrNull

@SpringBootTest
class CouponServiceConcurrencyTestIT @Autowired constructor(
    private val couponService: CouponService,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: JpaUserCouponRepository,
){
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Test
    fun `✅쿠폰 발급 동시성 테스트_100개의 쿠폰발급 동시요청이 들어와도 정확히 100개만 발급되어야 한다`() {
        // arrange: 쿠폰의 최대 발급 수량은 100개
        val coupon = CouponTestFixture.coupon(maxIssueLimit = 100).build()
        val savedCoupon = couponRepository.save(coupon)

        // act: 100개의 동시 요청
        executeConcurrently(100) { it ->
            val cmd = CouponCommand.Issue(it.toLong() + 1, savedCoupon.id!!)
            couponService.issueCoupon(cmd)
        }
        
        // assert: 쿠폰의 발급된 수량은 100개여야 한다.
        val findCoupon = couponRepository.getById(savedCoupon.id!!)
        val userCoupons = userCouponRepository.findAll()
        findCoupon.issuedCount shouldBe 100
        userCoupons shouldHaveSize 100
    }
    
    @Test
    fun `✅쿠폰 사용 동시성 테스트_같은 쿠폰에 대해 10번의 사용요청이 동시에 들어와도 1번만 사용되어야 한다`() {
        // arrange: 쿠폰 1개와 사용자 쿠폰 1개
        val coupon = couponRepository.save(CouponTestFixture.coupon(discountPolicy = CouponTestFixture.noConditionDiscountPolicy()).build())
        val userCoupon = userCouponRepository.save(CouponTestFixture.userCoupon(userId = 1L, coupon = coupon).build())
        val successCnt = AtomicInteger(0)
        val failureCnt = AtomicInteger(0)

        // act: 10번의 동시 사용 요청
        executeConcurrently(10) {
            try {
                couponService.use(createUseCouponCommand(1L, listOf(userCoupon.id!!)))
                successCnt.incrementAndGet()
            } catch(e: Throwable) {
                logger.error { e.message }
                failureCnt.incrementAndGet()
            }
        }

        // assert: successCnt는 1이어야 한다.
        successCnt.get() shouldBe 1
        failureCnt.get() shouldBe 9
        val findUserCoupon = userCouponRepository.findById(userCoupon.id!!).getOrNull()
        findUserCoupon!!.status shouldBe UserCouponStatus.USED
    }
    
    private fun createUseCouponCommand(userId: Long = 1L, userCouponIds: List<Long>) = CouponCommand.Use.Root(
        userId = 1L,
        userCouponIds = userCouponIds,
        totalAmount = Money.of(1000),
        items = listOf(CouponCommand.Use.Item(1L, 1L, 1L, 10, Money.of(1000))),
        timestamp = LocalDateTime.now(),
    )
}