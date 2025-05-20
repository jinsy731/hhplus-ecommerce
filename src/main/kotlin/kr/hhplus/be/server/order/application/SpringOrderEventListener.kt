package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.OrderEvent
import org.springframework.stereotype.Component

@Component
class SpringOrderEventListener(
    private val orderResultSender: OrderResultSender
) {
    fun onOrderCompleted(event: OrderEvent.Completed) {
        orderResultSender.send(event.payload.order)
    }
}