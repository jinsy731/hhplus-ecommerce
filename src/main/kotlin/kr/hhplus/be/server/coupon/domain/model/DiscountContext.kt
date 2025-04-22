package kr.hhplus.be.server.coupon.domain.model

import kr.hhplus.be.server.common.domain.Money
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 할인 적용 가능 조건을 체크하기 위한 컨텍스트 객체
 */
class DiscountContext {
    data class Root(
        val userId: Long? = null,
        val totalAmount: Money? = null,
        val items: List<Item>,
        val timestamp: LocalDateTime = LocalDateTime.now(),
        // 필요한 다른 컨텍스트 정보들을 추가할 수 있음
        val additionalContext: Map<String, Any> = emptyMap()
    )

    data class Item(
        val orderItemId: Long,
        val productId: Long,
        val variantId: Long,
        val quantity: Int,
        val subTotal: Money,
        val totalAmount: Money
    )
}