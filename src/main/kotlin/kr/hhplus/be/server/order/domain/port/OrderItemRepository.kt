package kr.hhplus.be.server.order.domain.port

import kr.hhplus.be.server.order.domain.model.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    
    /**
     * 주문 ID로 주문 항목 목록 조회
     */
    fun findByOrderId(orderId: Long): List<OrderItem>
    
    /**
     * 주문 ID 목록으로 주문 항목 목록 조회
     */
    fun findByOrderIdIn(orderIds: List<Long>): List<OrderItem>
    
    /**
     * 상품 ID로 주문 항목 목록 조회
     */
    fun findByProductId(productId: Long): List<OrderItem>
}
