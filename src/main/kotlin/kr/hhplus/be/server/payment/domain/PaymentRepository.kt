package kr.hhplus.be.server.payment.domain

import kr.hhplus.be.server.payment.domain.model.Payment
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository {

    fun save(payment: Payment): Payment

    fun getById(id: Long): Payment
    fun getByOrderId(orderId: Long): Payment
}