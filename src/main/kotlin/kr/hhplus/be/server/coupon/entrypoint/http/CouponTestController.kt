package kr.hhplus.be.server.coupon.entrypoint.http

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import kr.hhplus.be.server.coupon.domain.model.FixedAmountTotalDiscountType
import kr.hhplus.be.server.coupon.domain.model.MinOrderAmountCondition
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.shared.domain.Money
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.*


@RestController
@RequestMapping(("/api/test/coupons"))
class CouponTestController(private val couponRepository: CouponRepository) {

    @PostMapping
    fun createTestCoupon(): ResponseEntity<Long> {
        val savedCoupon = couponRepository.save(
            Coupon(
                name = UUID.randomUUID().toString(),
                description = "테스트 쿠폰",
                discountPolicy = DiscountPolicy(
                    name = "테스트 정책",
                    discountType = FixedAmountTotalDiscountType(Money.of(10000)),
                    discountCondition = MinOrderAmountCondition(Money.of(10000))
                ),
                isActive = true,
                maxIssueLimit = 10000000,
                issuedCount = 0,
                startAt = LocalDateTime.now(),
                endAt = LocalDateTime.now().plusHours(1),
                validDays = 10,
            )
        )

        return ResponseEntity.ok(savedCoupon.id)
    }
}