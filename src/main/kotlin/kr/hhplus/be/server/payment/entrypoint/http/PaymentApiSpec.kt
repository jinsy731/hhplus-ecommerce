package kr.hhplus.be.server.payment.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.common.Response
import kr.hhplus.be.server.payment.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Payment", description = "결제 API")
interface PaymentApiSpec {

    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다")
    @PostMapping
    fun createPayment(
        @RequestBody request: PaymentCreateRequest
    ): ResponseEntity<Response<PaymentResponse>>

    @Operation(summary = "결제 조회", description = "결제 ID로 결제 정보를 조회합니다")
    @GetMapping("/{paymentId}")
    fun getPayment(
        @Parameter(description = "결제 ID") @PathVariable paymentId: Long
    ): ResponseEntity<Response<PaymentResponse>>

    @Operation(summary = "주문별 결제 조회", description = "주문 ID로 결제 정보를 조회합니다")
    @GetMapping("/order/{orderId}")
    fun getPaymentByOrderId(
        @Parameter(description = "주문 ID") @PathVariable orderId: Long
    ): ResponseEntity<Response<PaymentResponse>>

    @Operation(summary = "결제 환불", description = "결제를 전체 또는 부분 환불합니다")
    @PostMapping("/{paymentId}/refund")
    fun refundPayment(
        @Parameter(description = "결제 ID") @PathVariable paymentId: Long,
        @RequestBody request: PaymentRefundRequest
    ): ResponseEntity<Response<PaymentResponse>>
}
