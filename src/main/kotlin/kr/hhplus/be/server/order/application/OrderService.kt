package kr.hhplus.be.server.order.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.port.OrderRepository
import org.springframework.stereotype.Service

@Service
class OrderService(private val orderRepository: OrderRepository) {

    @Transactional
    fun createOrder(cmd: OrderCommand.Create): Order = Order.create(cmd)

    @Transactional
    fun applyDiscount(cmd: OrderCommand.ApplyDiscount) {
        cmd.order.applyDiscount(cmd.discountLines)
    }

    @Transactional
    fun save(order: Order): Order = orderRepository.save(order)
}