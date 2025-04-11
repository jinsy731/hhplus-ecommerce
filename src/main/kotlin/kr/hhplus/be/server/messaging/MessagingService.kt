package kr.hhplus.be.server.messaging

import kr.hhplus.be.server.order.domain.model.Order

interface MessagingService {
    fun publish(order: Order)
}