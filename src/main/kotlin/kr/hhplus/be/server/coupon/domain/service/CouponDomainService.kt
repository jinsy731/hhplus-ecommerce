package kr.hhplus.be.server.coupon.domain.service

import kr.hhplus.be.server.coupon.domain.model.*
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import org.springframework.stereotype.Component

/**
 * 쿠폰 관련 도메인 서비스
 * 복잡한 비즈니스 로직이나 여러 엔티티가 협력하는 로직을 담당
 */
@Component
class CouponDomainService(
    private val couponRepository: CouponRepository
) {
    
    /**
     * 사용자 쿠폰의 할인 계산 및 사용 처리
     * 도메인 로직을 캡슐화하여 애플리케이션 레이어에서 호출
     */
    fun calculateDiscountAndUse(
        userCoupon: UserCoupon, 
        context: DiscountContext.Root, 
        orderId: Long
    ): List<DiscountLine> {
        // 쿠폰 조회
        val coupon = couponRepository.getById(userCoupon.couponId)
        
        // 쿠폰 유효성 검증
        coupon.validatUsability(context.timestamp)
        
        // 적용 가능한 아이템 조회
        val applicableItems = coupon.getApplicableItems(context)

        // 쿠폰 사용 처리
        userCoupon.use(context.timestamp, orderId)

        // 할인 금액 계산
        val orderItemsDiscountMap = coupon.calculateDiscount(context, applicableItems)
        
        return DiscountLine.from(
            sourceId = coupon.id!!,
            discountMethod = DiscountMethod.COUPON,
            orderItemsDiscountMap = orderItemsDiscountMap,
            now = context.timestamp
        )
    }
    
    /**
     * 쿠폰 정보 조회
     * 매퍼나 다른 레이어에서 필요한 쿠폰 정보를 제공
     */
    fun getCouponInfo(couponId: Long): Coupon {
        return couponRepository.getById(couponId)
    }
} 