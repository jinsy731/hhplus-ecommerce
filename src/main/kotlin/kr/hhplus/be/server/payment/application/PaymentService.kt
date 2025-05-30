package kr.hhplus.be.server.payment.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.payment.domain.PaymentEventPublisher
import kr.hhplus.be.server.payment.domain.PaymentRepository
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.infrastructure.client.PgClient
import kr.hhplus.be.server.payment.infrastructure.client.PgPaymentRequest
import kr.hhplus.be.server.payment.infrastructure.client.PgPaymentStatus
import kr.hhplus.be.server.shared.exception.AlreadyPaidException
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher,
    private val pgClient: PgClient
) {

    @Transactional
    fun preparePayment(cmd: PaymentCommand.Prepare.Root): Result<Payment> {
        return runCatching {
            // 중복 결제 방지 - 해당 주문 ID로 이미 성공한 결제가 있는지 확인
            if (paymentRepository.hasSuccessfulPaymentByOrderId(cmd.order.id)) {
                throw AlreadyPaidException()
            }

            val payment = Payment.create(cmd.toPreparePaymentContext())
            paymentRepository.save(payment)
        }
    }

    @Transactional
    fun processPaymentWithPg(cmd: PaymentCommand.ProcessWithPg): Result<PaymentResult.ProcessWithPg> {
        return runCatching {
            // 0. 중복 결제 방지 - 해당 주문 ID로 이미 성공한 결제가 있는지 확인
            if (paymentRepository.hasSuccessfulPaymentByOrderId(cmd.orderId)) {
                throw AlreadyPaidException()
            }
            
            // 1. PG 결제 처리
            val pgRequest = PgPaymentRequest(
                pgPaymentId = cmd.pgPaymentId,
                amount = cmd.amount,
                paymentMethod = cmd.paymentMethod,
                orderId = cmd.orderId
            )
            
            val pgResult = pgClient.processPayment(pgRequest)
            
            // 2. PG 결제 실패 시 예외 발생
            if (pgResult.status != PgPaymentStatus.SUCCESS) {
                throw RuntimeException("PG 결제 실패: ${pgResult.message}")
            }
            
            // 3. Payment 완료 처리
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.complete()
            paymentRepository.save(payment)
            
            // 4. 결과 반환
            PaymentResult.ProcessWithPg(
                paymentId = cmd.paymentId,
                orderId = cmd.orderId,
                pgPaymentId = pgResult.pgPaymentId,
                paidAmount = pgResult.paidAmount,
                status = when (pgResult.status) {
                    PgPaymentStatus.SUCCESS -> PaymentStatus.SUCCESS
                    PgPaymentStatus.FAILED -> PaymentStatus.FAILED
                    PgPaymentStatus.PENDING -> PaymentStatus.PENDING
                    PgPaymentStatus.CANCELLED -> PaymentStatus.CANCELLED
                },
                paidAt = pgResult.paidAt,
                message = pgResult.message
            )
        }
    }

    @Transactional
    fun completePayment(cmd: PaymentCommand.Complete): Result<Payment> {
        return runCatching {
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.complete()
            paymentRepository.save(payment)
        }
    }

    @Transactional
    fun cancelPayment(cmd: PaymentCommand.Cancel): Result<Payment> {
        return runCatching {
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.cancel()
            paymentRepository.save(payment)
        }
    }

    @Transactional
    fun failPayment(cmd: PaymentCommand.Fail): Result<PaymentResult.Fail> {
        return runCatching {
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.fail()
            paymentRepository.save(payment)
            
            PaymentResult.Fail(
                paymentId = cmd.paymentId,
                orderId = cmd.orderId,
                status = PaymentStatus.FAILED,
                failedAt = cmd.timestamp,
                message = cmd.failedReason
            )
        }.onFailure { e ->
            paymentEventPublisher.publishPaymentFailureFailed(createEventPayload(cmd.orderId, cmd.paymentId, cmd.pgPaymentId, cmd.amount, cmd.timestamp, "결제 실패 처리 중 오류: ${e.message}"))
        }.onSuccess { result ->
            paymentEventPublisher.publishPaymentFailed(createEventPayload(cmd.orderId, cmd.paymentId, cmd.pgPaymentId, cmd.amount, cmd.timestamp, cmd.failedReason))
        }
    }
    
    private fun createEventPayload(
        orderId: Long,
        paymentId: Long?,
        pgPaymentId: String?,
        amount: kr.hhplus.be.server.shared.domain.Money,
        timestamp: java.time.LocalDateTime,
        failedReason: String? = null
    ) = kr.hhplus.be.server.payment.domain.PaymentEventPayload(
        orderId = orderId,
        paymentId = paymentId,
        pgPaymentId = pgPaymentId,
        amount = amount,
        timestamp = timestamp,
        failedReason = failedReason
    )
}