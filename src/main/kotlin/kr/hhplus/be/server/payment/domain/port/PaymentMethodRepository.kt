package kr.hhplus.be.server.payment.domain.port

import kr.hhplus.be.server.payment.domain.model.PaymentMethodType
import kr.hhplus.be.server.payment.domain.model.PaymentMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentMethodRepository : JpaRepository<PaymentMethod, Long> {
    
    /**
     * 결제 ID로 모든 결제 수단 조회
     */
    fun findAllByPaymentId(paymentId: Long): List<PaymentMethod>
    
    /**
     * 결제 ID와 결제 수단 유형으로 결제 수단 조회
     */
    fun findAllByPaymentIdAndType(paymentId: Long, type: PaymentMethodType): List<PaymentMethod>
}
