package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.order.domain.OrderStatus


@Schema(description = "주문 항목 요청 정보")
data class OrderItemRequest(

    @Schema(description = "상품 ID", example = "1")
    val productId: Long,

    @Schema(description = "상품 옵션 조합 ID", example = "101")
    val variantId: Long,

    @Schema(description = "주문 수량", example = "2")
    val quantity: Int
)


@Schema(description = "주문 생성 요청")
data class CreateOrderRequest(

    @Schema(description = "유저 ID", example = "1")
    val userId: Long,

    @Schema(description = "주문 항목 리스트")
    val items: List<OrderItemRequest>,

    @Schema(description = "사용할 유저 쿠폰 ID (nullable)", example = "123", nullable = true)
    val userCouponId: Long?
)

@Schema(description = "주문 생성 응답")
data class CreateOrderResponse(

    @Schema(description = "주문 ID", example = "999")
    val orderId: Long,

    @Schema(description = "주문 상태", example = "PAID")
    val status: OrderStatus,

    @Schema(description = "최종 결제 금액", example = "15000")
    val finalTotal: Int
)
