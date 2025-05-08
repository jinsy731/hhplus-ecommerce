package kr.hhplus.be.server.coupon.infrastructure

import io.kotest.matchers.nulls.shouldNotBeNull
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class CouponRepositoryTestIT @Autowired constructor(
    private val couponRepository: CouponRepository,
    private val txTemplate: TransactionTemplate
){
    
    @Test
    fun `✅쿠폰 조회(for update)`() {
        // arrange: 쿠폰은 100개의 최대 발급 수량을 가진다.
        val coupon = CouponTestFixture.coupon(maxIssueLimit = 100).build()
        var couponId: Long = 0L
        txTemplate.execute { couponId = couponRepository.save(coupon).id!! }
        
        // act: 쿠폰 조회
        val findCoupon = txTemplate.execute { couponRepository.getByIdForUpdate(couponId) }
        
        // assert
        findCoupon.shouldNotBeNull()
    }
}