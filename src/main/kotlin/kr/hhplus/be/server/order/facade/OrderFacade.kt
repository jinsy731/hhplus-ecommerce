package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.product.application.ProductService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    ) {

    @Transactional
    fun placeOrder(cri: OrderCriteria.PlaceOrder.Root): Order {
        val products = productService.findAllById(cri.items.map { it.productId })
        return orderService.createOrder(cri.toCreateOrderCommand(products))
    }
}