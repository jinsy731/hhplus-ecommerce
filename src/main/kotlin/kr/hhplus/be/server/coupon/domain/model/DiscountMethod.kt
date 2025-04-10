package kr.hhplus.be.server.coupon.domain.model

/**
 * 할인 유형
 */
enum class DiscountMethod {
    /**
     * 쿠폰 할인
     */
    COUPON,

    /**
     * 프로모션 할인
     */
    PROMOTION,
}