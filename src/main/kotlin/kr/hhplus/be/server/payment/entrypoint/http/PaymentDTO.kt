package kr.hhplus.be.server.payment.entrypoint.http

import kr.hhplus.be.server.payment.domain.model.PaymentMethodType
import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentItemDetail
import kr.hhplus.be.server.payment.domain.model.PaymentMethod
import java.time.LocalDateTime

/**
 * 결제 생성 요청 DTO
 */
data class PaymentCreateRequest(
    val orderId: Long,
    val originalTotal: Int,
    val finalTotal: Int,
    val details: List<PaymentItemDetailRequest>,
    val methods: List<PaymentMethodRequest>
)

/**
 * 결제 항목 상세 요청 DTO
 */
data class PaymentItemDetailRequest(
    val orderItemId: Long,
    val originalPrice: Int,
    val discountedPrice: Int
)

/**
 * 결제 수단 요청 DTO
 */
data class PaymentMethodRequest(
    val type: PaymentMethodType,
    val amount: Int,
    val identifier: String? = null,
    val metadata: String? = null
)

/**
 * 결제 환불 요청 DTO
 */
data class PaymentRefundRequest(
    val amount: Int,
    val orderItemIds: List<Long> = emptyList(),
    val reason: String? = null
)

/**
 * 결제 응답 DTO
 */
data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val originalTotal: Int,
    val finalTotal: Int,
    val nonCashAmount: Int,
    val paidAmount: Int,
    val refundedAmount: Int,
    val status: PaymentStatus,
    val timestamp: LocalDateTime?,
    val details: List<PaymentItemDetailResponse>,
    val methods: List<PaymentMethodResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                id = payment.id,
                orderId = payment.orderId,
                originalTotal = payment.originalTotal,
                finalTotal = payment.finalTotal,
                nonCashAmount = payment.nonCashAmount,
                paidAmount = payment.paidAmount,
                refundedAmount = payment.refundedAmount,
                status = payment.status,
                timestamp = payment.timestamp,
                details = payment.details.map { PaymentItemDetailResponse.from(it) },
                methods = payment.methods.map { PaymentMethodResponse.from(it) },
                createdAt = payment.createdAt,
                updatedAt = payment.updatedAt
            )
        }
    }
}

/**
 * 결제 항목 상세 응답 DTO
 */
data class PaymentItemDetailResponse(
    val id: Long,
    val orderItemId: Long,
    val originalPrice: Int,
    val discountedPrice: Int,
    val nonCashAmount: Int,
    val paidAmount: Int,
    val refunded: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(detail: PaymentItemDetail): PaymentItemDetailResponse {
            return PaymentItemDetailResponse(
                id = detail.id,
                orderItemId = detail.orderItemId,
                originalPrice = detail.originalPrice,
                discountedPrice = detail.discountedPrice,
                nonCashAmount = detail.nonCashAmount,
                paidAmount = detail.paidAmount,
                refunded = detail.refunded,
                createdAt = detail.createdAt,
                updatedAt = detail.updatedAt
            )
        }
    }
}

/**
 * 결제 수단 응답 DTO
 */
data class PaymentMethodResponse(
    val id: Long,
    val type: PaymentMethodType,
    val amount: Int,
    val identifier: String?,
    val metadata: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(method: PaymentMethod): PaymentMethodResponse {
            return PaymentMethodResponse(
                id = method.id,
                type = method.type,
                amount = method.amount,
                identifier = method.identifier,
                metadata = method.metadata,
                createdAt = method.createdAt,
                updatedAt = method.updatedAt
            )
        }
    }
}
