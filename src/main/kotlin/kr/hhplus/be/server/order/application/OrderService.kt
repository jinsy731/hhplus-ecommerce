package kr.hhplus.be.server.order.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderRepository
import org.springframework.stereotype.Service

@Service
class OrderService(private val orderRepository: OrderRepository) {

    @Transactional
    fun createOrder(cmd: OrderCommand.Create): Order {
        val order = Order.create(cmd)
        return orderRepository.save(order)
    }

    @Transactional
    fun completeOrder(order: Order) {
        order.completeOrder()
    }
}