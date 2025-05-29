package kr.hhplus.be.server.coupon.application.dto

import kr.hhplus.be.server.shared.domain.Money

/**
 * 할인 정보를 나타내는 인터페이스
 * Coupon 도메인과의 의존성을 줄이기 위한 추상화
 */
data class DiscountInfo (
    val orderItemId: Long,
    val amount: Money,
    val sourceId: Long,
    val sourceType: String
)

// 매핑 함수는 CouponMapper로 이동됨
