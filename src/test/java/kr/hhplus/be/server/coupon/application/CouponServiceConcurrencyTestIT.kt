package kr.hhplus.be.server.coupon.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.infrastructure.JpaUserCouponRepository
import kr.hhplus.be.server.executeConcurrently
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CouponServiceConcurrencyTestIT @Autowired constructor(
    private val couponService: CouponService,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: JpaUserCouponRepository,
){
    
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
}