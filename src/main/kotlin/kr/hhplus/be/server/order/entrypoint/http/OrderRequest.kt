package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.order.facade.OrderCriteria

class OrderRequest {
    class CreateOrderSheet {
        @Schema(description = "주문서 생성 요청")
        data class Root(
            @Schema(description = "유저 ID", example = "1")
            val userId: Long,

            @Schema(description = "주문 항목 리스트")
            val items: List<OrderItem>,

            @Schema(description = "사용할 유저 쿠폰 ID ", example = "[1,2,3]",)
            val userCouponIds: List<Long>,
        )

        @Schema(description = "주문 항목 요청 정보")
        data class OrderItem(
            @Schema(description = "상품 ID", example = "1")
            val productId: Long,

            @Schema(description = "상품 옵션 조합 ID", example = "101")
            val variantId: Long,

            @Schema(description = "주문 수량", example = "2")
            val quantity: Int
        )
    }

    class ProcessPayment {
        @Schema(description = "결제 처리 요청")
        data class Root(
            @Schema(description = "PG 결제 ID", example = "pg_payment_12345")
            val pgPaymentId: String,

            @Schema(description = "결제 수단", example = "CARD")
            val paymentMethod: String
        )
    }

    class Create {
        @Schema(description = "주문 생성 요청")
        data class Root(
            @Schema(description = "유저 ID", example = "1")
            val userId: Long,

            @Schema(description = "주문 항목 리스트")
            val items: List<OrderItem>,

            @Schema(description = "사용할 유저 쿠폰 ID ", example = "[1,2,3]",)
            val userCouponIds: List<Long>,
        )


        @Schema(description = "주문 항목 요청 정보")
        data class OrderItem(
            @Schema(description = "상품 ID", example = "1")
            val productId: Long,

            @Schema(description = "상품 옵션 조합 ID", example = "101")
            val variantId: Long,

            @Schema(description = "주문 수량", example = "2")
            val quantity: Int
        )
    }
}

// 주문서 생성 매핑
fun OrderRequest.CreateOrderSheet.Root.toCreateOrderSheetCriteria() = OrderCriteria.CreateOrderSheet.Root(
    userId = this.userId,
    items = this.items.toOrderSheetItemCriteria(),
    userCouponIds = this.userCouponIds,
)

fun List<OrderRequest.CreateOrderSheet.OrderItem>.toOrderSheetItemCriteria() = this.map { OrderCriteria.CreateOrderSheet.Item(
    productId = it.productId,
    variantId = it.variantId,
    quantity = it.quantity
)}

// 결제 처리 매핑
fun OrderRequest.ProcessPayment.Root.toProcessPaymentCriteria(orderId: Long) = OrderCriteria.ProcessPayment.Root(
    orderId = orderId,
    pgPaymentId = this.pgPaymentId,
    paymentMethod = this.paymentMethod
)

// 기존 주문 생성 매핑 (호환성 유지)
fun OrderRequest.Create.Root.toCreateCriteria() = OrderCriteria.PlaceOrder.Root(
    userId = this.userId,
    items = this.items.toOrderItemCriteria(),
    userCouponIds = this.userCouponIds,
)

fun List<OrderRequest.Create.OrderItem>.toOrderItemCriteria() = this.map { OrderCriteria.PlaceOrder.Item(
    productId = it.productId,
    variantId = it.variantId,
    quantity = it.quantity
)}
