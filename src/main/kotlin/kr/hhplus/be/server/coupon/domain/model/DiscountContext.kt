package kr.hhplus.be.server.coupon.domain.model

import java.math.BigDecimal

/**
 * 할인 적용 가능 조건을 체크하기 위한 컨텍스트 객체
 */
data class DiscountContext(
    val userId: Long? = null,
    val productId: Long? = null,
    val variantId: Long? = null,
    val orderAmount: BigDecimal? = null,
    // 필요한 다른 컨텍스트 정보들을 추가할 수 있음
    val additionalContext: Map<String, Any> = emptyMap()
)
