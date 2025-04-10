package kr.hhplus.be.server.payment.entrypoint.http

import kr.hhplus.be.server.common.Response
import kr.hhplus.be.server.payment.dto.*
import kr.hhplus.be.server.temp.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService
) : PaymentApiSpec {

    override fun createPayment(request: PaymentCreateRequest): ResponseEntity<Response<PaymentResponse>> {
        val payment = paymentService.createPayment(request)
        return ResponseEntity.ok(Response.success(PaymentResponse.from(payment)))
    }

    override fun getPayment(paymentId: Long): ResponseEntity<Response<PaymentResponse>> {
        val payment = paymentService.getPayment(paymentId)
        return ResponseEntity.ok(Response.success(PaymentResponse.from(payment)))
    }

    override fun getPaymentByOrderId(orderId: Long): ResponseEntity<Response<PaymentResponse>> {
        val payment = paymentService.getPaymentByOrderId(orderId)
        return ResponseEntity.ok(Response.success(PaymentResponse.from(payment)))
    }

    override fun refundPayment(paymentId: Long, request: PaymentRefundRequest): ResponseEntity<Response<PaymentResponse>> {
        val payment = paymentService.refundPayment(paymentId, request)
        return ResponseEntity.ok(Response.success(PaymentResponse.from(payment)))
    }
}
