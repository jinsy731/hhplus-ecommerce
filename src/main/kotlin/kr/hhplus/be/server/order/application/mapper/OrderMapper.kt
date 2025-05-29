package kr.hhplus.be.server.order.application.mapper

import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItems
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OrderMapper {

    /**
     * 쿠폰 사용 명령어 생성
     */
    fun mapToCouponUseCommand(
        order: Order,
        userCouponIds: List<Long>,
        timestamp: LocalDateTime
    ): CouponCommand.Use.Root {
        return CouponCommand.Use.Root(
            orderId = order.id!!,
            userId = order.userId,
            userCouponIds = userCouponIds,
            totalAmount = order.originalTotal,
            items = order.orderItems.asList().map { orderItem ->
                CouponCommand.Use.Item(
                    orderItemId = orderItem.id ?: throw IllegalStateException("OrderItem id must not be null after save"),
                    productId = orderItem.productId,
                    variantId = orderItem.variantId,
                    quantity = orderItem.quantity,
                    subTotal = orderItem.unitPrice * orderItem.quantity.toBigDecimal()
                )
            },
            timestamp = timestamp,
        )
    }

    fun OrderItems.toUseCouponCommandItem(): List<CouponCommand.Use.Item> = this.asList().map { orderItem -> CouponCommand.Use.Item(
        orderItemId = orderItem.id ?: throw IllegalStateException("OrderItem id must not be null after save"),
        productId = orderItem.productId,
        variantId = orderItem.variantId,
        quantity = orderItem.quantity,
        subTotal = orderItem.subTotal
    ) }
} 