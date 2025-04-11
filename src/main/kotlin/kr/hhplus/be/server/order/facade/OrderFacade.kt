package kr.hhplus.be.server.order.facade

import jakarta.transaction.Transactional
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.messaging.MessagingService
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.user.application.UserPointCommand
import kr.hhplus.be.server.user.application.UserPointService
import org.springframework.stereotype.Service

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userPointService: UserPointService,
    private val paymentService: PaymentService,
    private val productService: ProductService,
    private val messagingService: MessagingService
    ) {

    @Transactional
    fun placeOrder(cri: OrderCriteria.PlaceOrder) {
        val products = productService.findAllById(cri.orderItem.map { it.productId })
        val order = orderService.createOrder(cri.toOrderCommand(products))
        val applyCouponResult = couponService.applyCoupon(cri.toCouponCommand(order))
        orderService.applyDiscount(OrderCommand.ApplyDiscount(order, applyCouponResult.discountLine))
        val payment = paymentService.preparePayment(cri.toPreparePaymentCommand(order))
        paymentService.completePayment(PaymentCommand.Complete(payment.id))
        orderService.completeOrder(order)
        userPointService.use(UserPointCommand.Use(cri.userId, order.finalTotal(), cri.now))
        messagingService.publish(order)
    }
}