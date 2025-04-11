package kr.hhplus.be.server.common

import kr.hhplus.be.server.order.domain.Order

interface MessagingService {
    fun publish(order: Order)
}