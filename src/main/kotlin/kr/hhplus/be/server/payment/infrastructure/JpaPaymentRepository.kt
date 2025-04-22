package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.payment.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPaymentRepository: JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Payment?
}