package kr.hhplus.be.server.payment.domain.port

import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import kr.hhplus.be.server.payment.domain.model.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    
    /**
     * 주문 ID로 결제 정보 조회
     */
    fun findByOrderId(orderId: Long): Optional<Payment>
    
    /**
     * 주문 ID와 결제 상태로 결제 정보 조회
     */
    fun findByOrderIdAndStatus(orderId: Long, status: PaymentStatus): Optional<Payment>
    
    /**
     * 결제 상태별 모든 결제 조회
     */
    fun findAllByStatus(status: PaymentStatus): List<Payment>
}
