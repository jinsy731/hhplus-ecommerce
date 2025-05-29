package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload

interface OrderResultSender {
    fun send(orderCompletedPayload: OrderCompletedPayload)
}