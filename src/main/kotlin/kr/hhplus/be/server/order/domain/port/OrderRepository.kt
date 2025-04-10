package kr.hhplus.be.server.order.domain.port

import kr.hhplus.be.server.order.domain.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    /**
     * 사용자 ID로 주문 목록 조회
     */
    fun findByUserId(userId: Long): List<Order>
    
    /**
     * 사용자 ID와 주문 ID로 주문 조회
     */
    fun findByIdAndUserId(id: Long, userId: Long): Optional<Order>
}
