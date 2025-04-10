package kr.hhplus.be.server.payment.domain.model

/**
 * 결제 상태를 나타내는 enum
 */
enum class PaymentStatus {
    PENDING,   // 결제 대기중
    PAID,      // 결제 완료
    PARTIALLY_REFUNDED,  // 부분 환불됨
    REFUNDED,  // 전액 환불됨
    FAILED     // 결제 실패
}
