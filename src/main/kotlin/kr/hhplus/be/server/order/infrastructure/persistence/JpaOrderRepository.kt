package kr.hhplus.be.server.order.infrastructure.persistence

import kr.hhplus.be.server.order.domain.model.Order
import org.springframework.data.jpa.repository.JpaRepository

interface JpaOrderRepository: JpaRepository<Order, Long> {
}