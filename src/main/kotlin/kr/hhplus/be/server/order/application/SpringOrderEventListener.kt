package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderResultSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringOrderEventListener(
    private val orderResultSender: OrderResultSender
) {
    @Async
    @TransactionalEventListener(OrderEvent.Completed::class)
    fun onOrderCompleted(event: OrderEvent.Completed) {
        orderResultSender.send(event.payload.order)
    }
}