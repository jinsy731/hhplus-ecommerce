package kr.hhplus.be.server.order.application

import kr.hhplus.be.server.coupon.application.CouponCommand
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.order.domain.OrderItems
import kr.hhplus.be.server.payment.application.PaymentCommand

fun OrderItems.toUseCouponCommandItem(): List<CouponCommand.Use.Item> = this.asList().map { orderItem -> CouponCommand.Use.Item(
    orderItemId = orderItem.id,
    productId = orderItem.productId,
    variantId = orderItem.variantId,
    quantity = orderItem.quantity,
    subTotal = orderItem.subTotal
) }

fun OrderItems.toPreparePaymentCommandItem(): List<PaymentCommand.Prepare.OrderItemInfo> = this.asList().map { orderItem -> PaymentCommand.Prepare.OrderItemInfo(
    id = orderItem.id,
    productId = orderItem.productId,
    variantId = orderItem.variantId,
    subTotal = orderItem.subTotal,
    discountedAmount = orderItem.discountAmount,
) }

