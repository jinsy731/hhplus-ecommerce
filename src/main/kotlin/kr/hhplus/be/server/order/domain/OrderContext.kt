package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * Order 도메인 계층의 VO
 * 주문 생성에 대한 Context를 담고 있음
 */
class OrderContext {
    class Create {
        data class Root(
            val userId: Long,
            val timestamp: LocalDateTime,
            val items: List<Item>
        )
        data class Item(
            val productId: Long,
            val variantId: Long,
            val quantity: Int,
            val unitPrice: Money
        )
    }
}