package kr.hhplus.be.server.payment.application

import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

class PaymentResult {
    
    /**
     * PG 처리 결과
     */
    data class ProcessWithPg(
        val paymentId: Long,
        val orderId: Long,
        val pgPaymentId: String,
        val paidAmount: Money,
        val status: PaymentStatus,
        val paidAt: LocalDateTime,
        val message: String
    )
    
    /**
     * 결제 실패 처리 결과
     */
    data class Fail(
        val paymentId: Long,
        val orderId: Long,
        val status: PaymentStatus,
        val failedAt: LocalDateTime,
        val message: String
    )
}

/**
 * 결제 상태
 */
enum class PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING,
    CANCELLED
} 