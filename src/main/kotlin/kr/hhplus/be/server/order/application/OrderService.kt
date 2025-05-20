package kr.hhplus.be.server.order.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.OrderRepository
import org.springframework.stereotype.Service

@Service
class OrderService(private val orderRepository: OrderRepository) {

    @Transactional
    fun createOrder(cmd: OrderCommand.Create.Root): Order {
        val order = Order.create(cmd.toOrderCreateContext())
        return orderRepository.save(order)
    }

    @Transactional
    fun completeOrder(orderId: Long) {
        val order = orderRepository.getById(orderId)
        order.completeOrder()
        orderRepository.save(order)
    }

    @Transactional
    fun applyDiscount(cmd: OrderCommand.ApplyDiscount) {
        val order = orderRepository.getById(cmd.orderId)
        order.applyDiscount(cmd.discountInfos)
        orderRepository.save(order)
    }
}