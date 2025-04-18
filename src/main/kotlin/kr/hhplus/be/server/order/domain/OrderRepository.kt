package kr.hhplus.be.server.order.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

interface OrderRepository{
    fun getById(id: Long): Order
    fun save(order: Order): Order
}