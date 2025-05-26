package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.model.Order

interface OrderResultSender {
    fun send(order: Order)
}