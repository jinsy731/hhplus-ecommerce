package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload
import kr.hhplus.be.server.order.domain.event.toOrderCompletedPayload
import kr.hhplus.be.server.order.domain.event.toOrderEventPayload
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: DomainEventPublisher
) {

    /**
     * 주문서 생성 (단순 생성만 담당)
     * - 복잡한 플로우는 OrderSagaOrchestrator에서 처리
     */
    @Transactional
    fun createOrderSheet(cmd: OrderCommand.CreateOrderSheet.Root): Order {
        val order = Order.create(cmd.toOrderCreateContext())
        return orderRepository.save(order)
    }

    /**
     * 주문에 할인 적용 (Saga에서 사용)
     */
    @Transactional
    fun applyDiscountToOrder(orderId: Long, discountInfos: List<DiscountInfo>) {
        val order = orderRepository.getById(orderId)
        order.applyDiscount(discountInfos)
        orderRepository.save(order)
    }



    /**
     * 기존 주문 생성 (호환성 유지)
     * - 기존 이벤트 기반 처리 방식
     */
    @Transactional
    fun createOrder(cmd: OrderCommand.Create.Root): Order {
        val order = Order.create(cmd.toOrderCreateContext())
        val savedOrder = orderRepository.save(order)

        eventPublisher.publish(OrderEvent.Created(
            savedOrder.toOrderEventPayload(timestamp = cmd.timestamp)
        ))

        return savedOrder
    }

    @Transactional
    fun completeOrder(orderId: Long, paymentCompletedPayload: PaymentCompletedPayload): Result<Order> {
        return runCatching {
            val order = orderRepository.getById(orderId)
            order.completeOrder()
            val completedOrder = orderRepository.save(order)
            
            // 주문 완료 이벤트 발행
            eventPublisher.publish(OrderEvent.Completed(
                completedOrder.toOrderCompletedPayload(
                    paymentId = paymentCompletedPayload.paymentId,
                    pgPaymentId = paymentCompletedPayload.pgPaymentId,
                    timestamp = paymentCompletedPayload.timestamp
                )
            ))
            
            completedOrder
        }.onFailure { e ->
            val order = orderRepository.getById(orderId)
            eventPublisher.publish(OrderEvent.Failed(
                order.toOrderEventPayload(
                    failedReason = e.message ?: "Unknown error",
                    timestamp = paymentCompletedPayload.timestamp
                )
            ))
        }
    }

    @Transactional
    fun fail(orderId: Long) {
        val order = orderRepository.getById(orderId)
        order.fail()
        orderRepository.save(order)
    }

    /**
     * 주문 조회 (Saga에서 사용)
     */
    @Transactional(readOnly = true)
    fun getOrderById(orderId: Long): Order {
        return orderRepository.getById(orderId)
    }

    /**
     * Saga에서 사용하기 위한 OrderInfo 조회
     * lazy loading 문제 없이 필요한 정보만 반환
     */
    @Transactional(readOnly = true)
    fun getOrderInfoById(orderId: Long): OrderInfo {
        val order = orderRepository.getById(orderId)
        return order.toOrderInfo()
    }

    /**
     * 주문 저장 (Saga에서 사용)
     */
    @Transactional
    fun saveOrder(order: Order): Order {
        return orderRepository.save(order)
    }
}