package kr.hhplus.be.server.payment.domain.port

import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import kr.hhplus.be.server.payment.domain.model.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository {

    fun save(payment: Payment): Payment

    fun getById(id: Long): Payment
}
