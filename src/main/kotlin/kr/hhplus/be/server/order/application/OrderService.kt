package kr.hhplus.be.server.order.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
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
    fun completeOrder(orderId: Long) {
        val order = orderRepository.getById(orderId)
        order.completeOrder()
        orderRepository.save(order)
    }

    @Transactional
    fun applyDiscount(orderId: Long, discountLines: List<DiscountLine>) {
        val order = orderRepository.getById(orderId)
        order.applyDiscount(discountLines)
        orderRepository.save(order)
    }
}