package kr.hhplus.be.server.payment.application

import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentItemDetail
import kr.hhplus.be.server.payment.domain.model.PaymentMethod
import kr.hhplus.be.server.payment.domain.port.PaymentRepository
import org.springframework.stereotype.Service

@Service
class PaymentService(private val paymentRepository: PaymentRepository) {

    fun preparePayment(cmd: PaymentCommand.Prepare) {

    }
}