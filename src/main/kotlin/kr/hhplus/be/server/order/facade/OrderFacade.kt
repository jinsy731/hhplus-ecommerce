package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.shared.messaging.MessagingService
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.product.application.ProductCommand
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.point.application.UserPointCommand
import kr.hhplus.be.server.point.application.UserPointService
import kr.hhplus.be.server.rank.application.RankingCommand
import kr.hhplus.be.server.rank.application.RankingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userPointService: UserPointService,
    private val paymentService: PaymentService,
    private val productService: ProductService,
    private val messagingService: MessagingService,
    private val rankingService: RankingService
    ) {

    @Transactional
    fun placeOrder(cri: OrderCriteria.PlaceOrder.Root): Order {
        // 재고 차감
        productService.validateAndReduceStock(ProductCommand.ValidateAndReduceStock.Root(
            items = cri.items.map { ProductCommand.ValidateAndReduceStock.Item(
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity
            ) }
        ))
        val products = productService.findAllById(cri.items.map { it.productId })
        val order = orderService.createOrder(cri.toCreateOrderCommand(products))

        // 쿠폰 적용 및 포인트 차감
        val applyCouponResult = couponService.use(cri.toUseCouponCommand(order))
        orderService.applyDiscount(OrderCommand.ApplyDiscount(order.id, applyCouponResult.discountInfo))
        userPointService.use(UserPointCommand.Use(cri.userId, order.finalTotal(), cri.timestamp))

        // 결제 시작
        val payment = paymentService.preparePayment(cri.toPreparePaymentCommand(order))
        paymentService.completePayment(PaymentCommand.Complete(payment.id))
        orderService.completeOrder(order.id)

        messagingService.publish(order)
        rankingService.updateProductRanking(RankingCommand.UpdateProductRanking.Root(
            items = order.orderItems.map { RankingCommand.UpdateProductRanking.Item(it.productId, it.quantity.toLong()) },
            timestamp = cri.timestamp
        ))

        return order
    }
}