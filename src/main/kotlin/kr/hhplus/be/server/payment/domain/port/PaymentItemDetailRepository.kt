package kr.hhplus.be.server.payment.domain.port

import kr.hhplus.be.server.payment.domain.model.PaymentItemDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentItemDetailRepository : JpaRepository<PaymentItemDetail, Long> {
    
    /**
     * 결제 ID로 모든 결제 항목 상세 조회
     */
    fun findAllByPaymentId(paymentId: Long): List<PaymentItemDetail>
    
    /**
     * 주문 항목 ID로 결제 항목 상세 조회
     */
    fun findByOrderItemId(orderItemId: Long): PaymentItemDetail?
    
    /**
     * 특정 결제의 환불되지 않은 모든 항목 조회
     */
    fun findAllByPaymentIdAndRefundedFalse(paymentId: Long): List<PaymentItemDetail>
}
