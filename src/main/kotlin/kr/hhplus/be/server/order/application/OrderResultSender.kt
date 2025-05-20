package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.order.domain.model.Order

interface OrderResultSender {
    fun send(order: Order)
}