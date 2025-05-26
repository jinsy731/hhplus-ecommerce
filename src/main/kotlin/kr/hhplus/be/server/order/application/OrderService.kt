package kr.hhplus.be.server.order.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: DomainEventPublisher) {

    @Transactional
    fun createOrder(cmd: OrderCommand.Create.Root): Order {
        val order = Order.create(cmd.toOrderCreateContext())
        val savedOrder = orderRepository.save(order)

        eventPublisher.publish(OrderEvent.Created(OrderSagaContext(
            order = savedOrder,
            timestamp = cmd.timestamp
        )))

        return savedOrder
    }

    @Transactional
    fun completeOrder(orderId: Long, context: OrderSagaContext): Result<Order> {
        return runCatching {
            val order = orderRepository.getById(orderId)
            order.completeOrder()
            orderRepository.save(order)
        }.onFailure { e ->
            eventPublisher.publish(OrderEvent.Failed(context.copy(failedReason = e.message ?: "Unknown error")))
        }.onSuccess {
            eventPublisher.publish(OrderEvent.Completed(context))
        }
    }

    @Transactional
    fun applyDiscount(cmd: OrderCommand.ApplyDiscount): Result<Order> {
        return runCatching {
            val order = orderRepository.getById(cmd.orderId)
            order.applyDiscount(cmd.discountInfos)
            orderRepository.save(order)
        }.onFailure { e ->
            eventPublisher.publish(OrderEvent.CouponApplyFailed(cmd.context.copy(failedReason = e.message ?: "Unknown error")))
        }.onSuccess {
            eventPublisher.publish(OrderEvent.Prepared(cmd.context))
        }

    }

    @Transactional
    fun fail(orderId: Long) {
        val order = orderRepository.getById(orderId)
        order.fail()
        orderRepository.save(order)
    }
}