package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.payment.domain.Payment
import kr.hhplus.be.server.payment.domain.PaymentRepository
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
}