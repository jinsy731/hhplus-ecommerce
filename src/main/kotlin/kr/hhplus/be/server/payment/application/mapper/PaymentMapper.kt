package kr.hhplus.be.server.payment.application.mapper

import kr.hhplus.be.server.order.application.OrderInfo
import kr.hhplus.be.server.order.domain.client.PaymentStatus
import kr.hhplus.be.server.order.domain.client.ProcessPaymentRequest
import kr.hhplus.be.server.order.domain.client.ProcessPaymentResponse
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentResult
import kr.hhplus.be.server.payment.domain.PaymentRepository
import org.springframework.stereotype.Component

/**
 * Payment 도메인의 DTO 매핑을 담당하는 컴포넌트
 * PaymentClient에서 사용하는 복잡한 매핑 로직을 분리
 */
@Component
class PaymentMapper(
    private val paymentRepository: PaymentRepository
) {

    /**
     * OrderInfo를 Payment 준비 커맨드로 변환
     */
    fun toPaymentPrepareCommand(orderInfo: OrderInfo, timestamp: java.time.LocalDateTime): PaymentCommand.Prepare.Root {
        return PaymentCommand.Prepare.Root(
            order = PaymentCommand.Prepare.OrderInfo(
                id = orderInfo.id,
                userId = orderInfo.userId,
                items = orderInfo.items.map { orderItem ->
                    PaymentCommand.Prepare.OrderItemInfo(
                        id = orderItem.id,
                        productId = orderItem.productId,
                        variantId = orderItem.variantId,
                        subTotal = orderItem.subTotal,
                        discountedAmount = orderItem.discountAmount
                    )
                },
                originalTotal = orderInfo.originalTotal,
                discountedAmount = orderInfo.discountedAmount
            ),
            timestamp = timestamp
        )
    }

    /**
     * ProcessPaymentRequest를 PaymentCommand.ProcessWithPg로 변환
     */
    fun toProcessWithPgCommand(
        request: ProcessPaymentRequest,
        paymentId: Long
    ): PaymentCommand.ProcessWithPg {
        return PaymentCommand.ProcessWithPg(
            paymentId = paymentId,
            orderId = request.orderId,
            pgPaymentId = request.pgPaymentId,
            amount = request.amount,
            paymentMethod = request.paymentMethod,
            timestamp = request.timestamp
        )
    }

    /**
     * PaymentResult.ProcessWithPg를 ProcessPaymentResponse로 변환
     */
    fun toProcessPaymentResponse(result: PaymentResult.ProcessWithPg): ProcessPaymentResponse {
        return ProcessPaymentResponse(
            orderId = result.orderId,
            paymentId = result.paymentId,
            pgPaymentId = result.pgPaymentId,
            paidAmount = result.paidAmount,
            status = when (result.status) {
                kr.hhplus.be.server.payment.application.PaymentStatus.SUCCESS -> PaymentStatus.SUCCESS
                kr.hhplus.be.server.payment.application.PaymentStatus.FAILED -> PaymentStatus.FAILED
                kr.hhplus.be.server.payment.application.PaymentStatus.PENDING -> PaymentStatus.PENDING
                kr.hhplus.be.server.payment.application.PaymentStatus.CANCELLED -> PaymentStatus.CANCELLED
            },
            paidAt = result.paidAt,
            message = result.message
        )
    }

    /**
     * FailPaymentRequest를 PaymentCommand.Fail로 변환
     */
    fun toFailPaymentCommand(
        request: kr.hhplus.be.server.order.domain.client.FailPaymentRequest
    ): PaymentCommand.Fail {
        return PaymentCommand.Fail(
            paymentId = request.paymentId,
            orderId = request.orderId,
            pgPaymentId = request.pgPaymentId,
            amount = getPaymentAmount(request.paymentId), // Payment에서 금액 조회
            failedReason = request.failedReason,
            timestamp = request.timestamp
        )
    }

    /**
     * PaymentResult.Fail을 FailPaymentResponse로 변환
     */
    fun toFailPaymentResponse(result: PaymentResult.Fail): kr.hhplus.be.server.order.domain.client.FailPaymentResponse {
        return kr.hhplus.be.server.order.domain.client.FailPaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            status = when (result.status) {
                kr.hhplus.be.server.payment.application.PaymentStatus.SUCCESS -> PaymentStatus.SUCCESS
                kr.hhplus.be.server.payment.application.PaymentStatus.FAILED -> PaymentStatus.FAILED
                kr.hhplus.be.server.payment.application.PaymentStatus.PENDING -> PaymentStatus.PENDING
                kr.hhplus.be.server.payment.application.PaymentStatus.CANCELLED -> PaymentStatus.CANCELLED
            },
            failedAt = result.failedAt,
            message = result.message
        )
    }

    /**
     * Payment ID로부터 결제 금액을 조회
     */
    private fun getPaymentAmount(paymentId: Long): kr.hhplus.be.server.shared.domain.Money {
        val payment = paymentRepository.getById(paymentId)
        return payment.originalAmount.minus(payment.discountedAmount)
    }
} 