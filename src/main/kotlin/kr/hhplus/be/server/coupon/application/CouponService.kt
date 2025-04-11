package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderItem
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CouponService(
    private val userCouponRepository: UserCouponRepository,
    private val discountLineRepository: DiscountLineRepository,
    ) {

    /**
     * 쿠폰 적용 메서드
     * 1. 할인 적용 대상 필터링
     * 2. 적용 대상 전체에 대한 할인 금액 계산
     * 3. 각 대상에 할인 금액 분배 (물품별 할인 금액 계산을 위해)
     */
    fun applyCoupon(cmd: CouponCommand.ApplyToOrder): CouponResult.ApplyToOrder {
        val userCoupons = userCouponRepository.findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)

        val discountLines = userCoupons.flatMap {
            it.applyTo(cmd.order, cmd.userId, cmd.now)
        }

        cmd.order.applyDiscount(discountLines)

        discountLineRepository.saveAll(discountLines)

        return CouponResult.ApplyToOrder(discountLines)
    }
}
