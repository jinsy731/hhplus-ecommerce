package kr.hhplus.be.server.coupon.entrypoint.event

import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.order.domain.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringCouponProductEventListener(private val couponService: CouponService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderEvent.Created) {
        logger.info("[{}] Received OrderEvent.Created: orderId={}, userId={}, userCouponIds={}", 
            Thread.currentThread().name, event.payload.order.id, event.payload.order.userId, event.payload.userCouponIds)
        couponService.use(CouponCommand.Use.Root(
            userId = event.payload.order.userId,
            userCouponIds = event.payload.userCouponIds,
            totalAmount = event.payload.order.originalTotal,
            items = event.payload.order.orderItems.map { CouponCommand.Use.Item(
                orderItemId = it.id,
                productId = it.productId,
                variantId = it.variantId,
                quantity = it.quantity,
                subTotal = it.subTotal
            ) },
            timestamp = event.payload.timestamp,
            context = event.payload
        ))
    }
}