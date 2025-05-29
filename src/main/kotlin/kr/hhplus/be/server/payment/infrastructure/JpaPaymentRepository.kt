package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPaymentRepository: JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Payment?
    fun existsByOrderIdAndStatus(orderId: Long, status: PaymentStatus): Boolean
}