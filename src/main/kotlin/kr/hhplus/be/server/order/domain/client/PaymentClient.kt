package kr.hhplus.be.server.order.domain.client

import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * Order 도메인에서 Payment 도메인을 호출하기 위한 클라이언트 인터페이스
 */
interface PaymentClient {
    
    /**
     * 유저 포인트 차감
     */
    fun deductUserPoint(request: DeductUserPointRequest): Result<DeductUserPointResponse>
    
    /**
     * 결제 처리 (PG 연동)
     */
    fun processPayment(request: ProcessPaymentRequest): Result<ProcessPaymentResponse>
    
    /**
     * 유저 포인트 복구 (결제 실패 시)
     */
    fun restoreUserPoint(request: RestoreUserPointRequest): Result<Unit>

    /**
     * 결제 실패 처리
     */
    fun failPayment(request: FailPaymentRequest): Result<FailPaymentResponse>
}

/**
 * 유저 포인트 차감 요청
 */
data class DeductUserPointRequest(
    val orderId: Long,
    val userId: Long,
    val amount: Money,
    val timestamp: LocalDateTime
)

/**
 * 유저 포인트 차감 응답
 */
data class DeductUserPointResponse(
    val userId: Long,
    val deductedAmount: Money,
    val remainingBalance: Money
)

/**
 * 결제 처리 요청
 */
data class ProcessPaymentRequest(
    val orderId: Long,
    val userId: Long,
    val amount: Money,
    val pgPaymentId: String,
    val paymentMethod: String,
    val timestamp: LocalDateTime
)

/**
 * 결제 처리 응답
 */
data class ProcessPaymentResponse(
    val orderId: Long,
    val paymentId: Long,
    val pgPaymentId: String,
    val paidAmount: Money,
    val status: PaymentStatus,
    val paidAt: LocalDateTime,
    val message: String
)

/**
 * 결제 상태
 */
enum class PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING,
    CANCELLED
}

/**
 * 유저 포인트 복구 요청
 */
data class RestoreUserPointRequest(
    val orderId: Long,
    val userId: Long,
    val amount: Money,
    val timestamp: LocalDateTime
)

/**
 * 결제 실패 처리 요청
 */
data class FailPaymentRequest(
    val orderId: Long,
    val paymentId: Long,
    val pgPaymentId: String,
    val failedReason: String,
    val timestamp: LocalDateTime
)

/**
 * 결제 실패 처리 응답
 */
data class FailPaymentResponse(
    val paymentId: Long,
    val orderId: Long,
    val status: PaymentStatus,
    val failedAt: LocalDateTime,
    val message: String
)

/**
 * Order 도메인 모델을 클라이언트 요청으로 변환하는 확장 함수들
 */
fun Order.toDeductUserPointRequest(timestamp: LocalDateTime): DeductUserPointRequest {
    return DeductUserPointRequest(
        orderId = this.id!!,
        userId = this.userId,
        amount = this.finalTotal(),
        timestamp = timestamp
    )
}

fun Order.toProcessPaymentRequest(
    pgPaymentId: String,
    paymentMethod: String,
    timestamp: LocalDateTime
): ProcessPaymentRequest {
    return ProcessPaymentRequest(
        orderId = this.id!!,
        userId = this.userId,
        amount = this.finalTotal(),
        pgPaymentId = pgPaymentId,
        paymentMethod = paymentMethod,
        timestamp = timestamp
    )
}

fun Order.toRestoreUserPointRequest(timestamp: LocalDateTime): RestoreUserPointRequest {
    return RestoreUserPointRequest(
        orderId = this.id!!,
        userId = this.userId,
        amount = this.finalTotal(),
        timestamp = timestamp
    )
}

fun createFailPaymentRequest(
    orderId: Long,
    paymentId: Long,
    pgPaymentId: String,
    failedReason: String,
    timestamp: LocalDateTime
): FailPaymentRequest {
    return FailPaymentRequest(
        orderId = orderId,
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        failedReason = failedReason,
        timestamp = timestamp
    )
} 