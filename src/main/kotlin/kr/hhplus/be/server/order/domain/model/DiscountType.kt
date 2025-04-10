package kr.hhplus.be.server.order.domain.model

/**
 * 할인 유형
 */
enum class DiscountType {
    /**
     * 쿠폰 할인
     */
    COUPON,
    
    /**
     * 프로모션 할인
     */
    PROMOTION,
    
    /**
     * 포인트 사용
     */
    POINT
}
