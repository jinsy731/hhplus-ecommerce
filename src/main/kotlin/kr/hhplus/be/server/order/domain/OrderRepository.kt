package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.model.Order

interface OrderRepository{
    fun getById(id: Long): Order
    fun findAll(): List<Order>
    fun save(order: Order): Order
}