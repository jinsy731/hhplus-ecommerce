package kr.hhplus.be.server.payment.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.payment.domain.PaymentEvent
import kr.hhplus.be.server.payment.domain.PaymentRepository
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: DomainEventPublisher
    ) {

    @Transactional
    fun preparePayment(cmd: PaymentCommand.Prepare.Root): Result<Payment> {
        return runCatching {
            val payment = Payment.create(cmd.toPreparePaymentContext())
            paymentRepository.save(payment)
        }.onFailure { e ->
            eventPublisher.publish(PaymentEvent.InitializingFailed(cmd.context.copy(failedReason = e.message ?: "Unknown error")))
        }.onSuccess {
            eventPublisher.publish(PaymentEvent.Initialized(cmd.context.copy(paymentId = it.id)))
        }
    }

    @Transactional
    fun completePayment(cmd: PaymentCommand.Complete): Result<Payment> {
        return runCatching {
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.complete()
            paymentRepository.save(payment)
        }.onFailure { e ->
            eventPublisher.publish(PaymentEvent.Failed(cmd.context.copy(failedReason = e.message ?: "Unknown error")))
        }.onSuccess {
            eventPublisher.publish(PaymentEvent.Completed(cmd.context))
        }
    }

    @Transactional
    fun cancelPayment(cmd: PaymentCommand.Cancel): Result<Payment> {
        return runCatching {
            val payment = paymentRepository.getById(cmd.paymentId)
            payment.cancel()
            paymentRepository.save(payment)
        }.onFailure { e ->
            eventPublisher.publish(PaymentEvent.Failed(cmd.context.copy(failedReason = e.message ?: "Unknown error")))
        }.onSuccess {
            eventPublisher.publish(PaymentEvent.Canceled(cmd.context))
        }
    }
}