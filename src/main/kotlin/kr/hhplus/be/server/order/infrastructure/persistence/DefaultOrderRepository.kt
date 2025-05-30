package kr.hhplus.be.server.order.infrastructure.persistence

import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.OrderRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrElse

@Repository
class DefaultOrderRepository(private val jpaRepository: JpaOrderRepository): OrderRepository {
    override fun getById(id: Long): Order {
        return jpaRepository.findById(id).getOrElse { throw ResourceNotFoundException() }
    }

    override fun findAll(): List<Order> {
        return jpaRepository.findAll()
    }

    override fun save(order: Order): Order {
        return jpaRepository.save(order)
    }
}