package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.application.CouponCommand
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.payment.application.PaymentCommand

fun List<OrderItem>.toUseCouponCommandItem(): List<CouponCommand.Use.Item> = this.map { orderItem -> CouponCommand.Use.Item(
    orderItemId = orderItem.id,
    productId = orderItem.productId,
    variantId = orderItem.variantId,
    quantity = orderItem.quantity,
    subTotal = orderItem.subTotal()
) }

fun List<OrderItem>.toPreparePaymentCommandItem(): List<PaymentCommand.Prepare.OrderItemInfo> = this.map { orderItem -> PaymentCommand.Prepare.OrderItemInfo(
    id = orderItem.id,
    productId = orderItem.productId,
    variantId = orderItem.variantId,
    subTotal = orderItem.subTotal(),
    discountedAmount = orderItem.discountAmount,
) }

