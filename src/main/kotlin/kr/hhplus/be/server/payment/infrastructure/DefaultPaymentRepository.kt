package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.payment.domain.PaymentRepository
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrElse

@Repository
class DefaultPaymentRepository(private val jpaRepository: JpaPaymentRepository): PaymentRepository {
    override fun save(payment: Payment): Payment {
        return jpaRepository.save(payment)
    }

    override fun getById(id: Long): Payment {
        return jpaRepository.findById(id).getOrElse { throw ResourceNotFoundException() }
    }

    override fun getByOrderId(orderId: Long): Payment {
        return jpaRepository.findByOrderId(orderId) ?: throw ResourceNotFoundException()
    }
    
    override fun hasSuccessfulPaymentByOrderId(orderId: Long): Boolean {
        return jpaRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID)
    }
}