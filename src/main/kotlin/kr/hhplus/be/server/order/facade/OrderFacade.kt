package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.user.application.UserPointService
import org.springframework.stereotype.Service

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userPointService: UserPointService,
    private val paymentService: PaymentService,
    private val productService: ProductService,
    ) {

    fun placeOrder(cri: OrderCriteria.PlaceOrder) {
        val products = productService.findAllById(cri.orderItem.map { it.productId })
        val order = orderService.createOrder(cri.toOrderCommand(products))
        val applyCouponResult = couponService.applyCoupon(cri.toCouponCommand(order))
//        val discountedOrder = orderService.applyDiscount(OrderCommand.ApplyDiscount(order, applyCouponResult.))
    }
}