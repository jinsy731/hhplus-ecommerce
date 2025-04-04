package kr.hhplus.be.server.order

data class OrderItemRequest(
    val productId: Long,
    val variantId: Long,
    val quantity: Int
)

data class CreateOrderRequest(
    val userId: Long,
    val items: List<OrderItemRequest>,
    val userCouponId: Long?
)

data class CreateOrderResponse(
    val orderId: Long,
    val status: String,
    val finalTotal: Int
)
