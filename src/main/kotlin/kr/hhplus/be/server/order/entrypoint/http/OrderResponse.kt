package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderResponse {
    @Schema(description = "주문서 생성 응답")
    data class CreateOrderSheet(
        @Schema(description = "주문 ID", example = "999")
        val orderId: Long,

        @Schema(description = "주문 상태", example = "CREATED")
        val status: OrderStatus,

        @Schema(description = "원래 총액", example = "20000")
        val originalTotal: BigDecimal,

        @Schema(description = "할인 금액", example = "5000")
        val discountedAmount: BigDecimal,

        @Schema(description = "최종 결제 금액", example = "15000")
        val finalTotal: BigDecimal,

        @Schema(description = "주문서 생성 시간")
        val createdAt: LocalDateTime
    )

    @Schema(description = "결제 처리 응답")
    data class ProcessPayment(
        @Schema(description = "주문 ID", example = "999")
        val orderId: Long,

        @Schema(description = "주문 상태", example = "PAID")
        val status: OrderStatus,

        @Schema(description = "결제 금액", example = "15000")
        val paidAmount: BigDecimal,

        @Schema(description = "PG 결제 ID", example = "pg_payment_12345")
        val pgPaymentId: String,

        @Schema(description = "결제 완료 시간")
        val paidAt: LocalDateTime
    )

    @Schema(description = "주문 생성 응답")
    data class Create(
        @Schema(description = "주문 ID", example = "999")
        val orderId: Long,

        @Schema(description = "주문 상태", example = "PAID")
        val status: OrderStatus,

        @Schema(description = "최종 결제 금액", example = "15000")
        val finalTotal: BigDecimal
    )
}

// 주문서 생성 응답 매핑
fun Order.toCreateOrderSheetResponse() = OrderResponse.CreateOrderSheet(
    orderId = this.id!!,
    status = this.status,
    originalTotal = this.originalTotal.amount,
    discountedAmount = this.discountedAmount.amount,
    finalTotal = this.finalTotal().amount,
    createdAt = this.createdAt
)

// 결제 처리 응답 매핑
fun Order.toProcessPaymentResponse(pgPaymentId: String) = OrderResponse.ProcessPayment(
    orderId = this.id!!,
    status = this.status,
    paidAmount = this.finalTotal().amount,
    pgPaymentId = pgPaymentId,
    paidAt = this.updatedAt
)

// 기존 주문 생성 응답 매핑 (호환성 유지)
fun Order.toCreateResponse() = OrderResponse.Create(
    orderId = this.id!!,
    status = this.status,
    finalTotal = this.finalTotal().amount
)