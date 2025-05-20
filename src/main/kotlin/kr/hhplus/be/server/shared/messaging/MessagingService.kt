package kr.hhplus.be.server.shared.messaging

import kr.hhplus.be.server.order.domain.model.Order

interface MessagingService {
    fun publish(order: Order)
}