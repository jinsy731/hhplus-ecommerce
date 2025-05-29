package kr.hhplus.be.server.payment.infrastructure.client

import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * 외부 PG사와 연결하는 클라이언트 인터페이스
 * DIP를 준수하여 Payment 도메인이 외부 시스템에 의존하지 않도록 함
 */
interface PgClient {
    
    /**
     * PG를 통한 결제 처리
     */
    fun processPayment(request: PgPaymentRequest): PgPaymentResult
}

/**
 * PG 결제 요청
 */
data class PgPaymentRequest(
    val pgPaymentId: String,
    val amount: Money,
    val paymentMethod: String,
    val orderId: Long
)

/**
 * PG 결제 결과
 */
data class PgPaymentResult(
    val pgPaymentId: String,
    val status: PgPaymentStatus,
    val paidAmount: Money,
    val paidAt: LocalDateTime,
    val message: String
)

/**
 * PG 결제 상태
 */
enum class PgPaymentStatus {
    SUCCESS,
    FAILED,
    PENDING,
    CANCELLED
} 