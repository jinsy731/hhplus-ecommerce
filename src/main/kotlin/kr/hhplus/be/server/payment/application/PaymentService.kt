package kr.hhplus.be.server.payment.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.payment.domain.Payment
import kr.hhplus.be.server.payment.domain.PaymentRepository
import org.springframework.stereotype.Service

@Service
class PaymentService(private val paymentRepository: PaymentRepository) {

    @Transactional
    fun preparePayment(cmd: PaymentCommand.Prepare): Payment {
        return paymentRepository.save(Payment.create(cmd))
    }

    @Transactional
    fun completePayment(cmd: PaymentCommand.Complete) {
        val payment = paymentRepository.getById(cmd.paymentId)
        payment.completePayment()
    }

}