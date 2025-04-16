package kr.hhplus.be.server.order.facade

import jakarta.transaction.Transactional
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.common.MessagingService
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.coupon.application.toDiscountInfoList
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.product.application.ProductCommand
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
    fun placeOrder(cri: OrderCriteria.PlaceOrder.Root) {
        // 주문 생성
        productService.validatePurchasability(ProductCommand.ValidatePurchasability.Root(
            items = cri.items.map { ProductCommand.ValidatePurchasability.Item(
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity
            ) }
        ))
        val products = productService.findAllById(cri.items.map { it.productId })
        // TODO: 상품 검증(재고 + 상태)
        val order = orderService.createOrder(cri.toCreateOrderCommand(products))
        // 쿠폰 적용
        val applyCouponResult = couponService.use(cri.toUseCouponCommand(order))
        orderService.applyDiscount(OrderCommand.ApplyDiscount(order.id, applyCouponResult.discountInfo))
        // 결제 시작
        val payment = paymentService.preparePayment(cri.toPreparePaymentCommand(order))
        paymentService.completePayment(PaymentCommand.Complete(payment.id))
        orderService.completeOrder(order.id)
        productService.reduceStockByPurchase(ProductCommand.ReduceStockByPurchase.Root(
            items = order.orderItems.map { ProductCommand.ReduceStockByPurchase.Item(
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity
            ) }
        ))
        // TODO: 재고 차감
        userPointService.use(UserPointCommand.Use(cri.userId, order.finalTotal(), cri.timestamp))
        // 주문 메시지 전송
        messagingService.publish(order)
    }
}