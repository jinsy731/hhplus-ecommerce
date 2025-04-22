package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.order.facade.OrderCriteria
import java.math.BigDecimal

class OrderRequest {
    class Create {
        @Schema(description = "주문 생성 요청")
        data class Root(
            @Schema(description = "유저 ID", example = "1")
            val userId: Long,

            @Schema(description = "주문 항목 리스트")
            val items: List<OrderItem>,

            @Schema(description = "사용할 유저 쿠폰 ID ", example = "[1,2,3]",)
            val userCouponIds: List<Long>,

            @Schema(description = "결제 수단 리스트")
            val payMethods: List<PayMethod>
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

        @Schema(description = "결제 수단 정보")
        data class PayMethod(
            @Schema(description = "결제 수단", example = "POINT")
            val method: String,
            @Schema(description = "결제 금액", example = "10000")
            val amount: BigDecimal
        )
    }
}

fun OrderRequest.Create.Root.toCreateCriteria() = OrderCriteria.PlaceOrder.Root(
    userId = this.userId,
    items = this.items.toOrderItemCriteria(),
    userCouponIds = this.userCouponIds,
    payMethods = this.payMethods.toPayMethodCriteria()
)

fun List<OrderRequest.Create.OrderItem>.toOrderItemCriteria() = this.map { OrderCriteria.PlaceOrder.Item(
    productId = it.productId,
    variantId = it.variantId,
    quantity = it.quantity
)}

fun List<OrderRequest.Create.PayMethod>.toPayMethodCriteria() = this.map { OrderCriteria.PlaceOrder.PayMethod(
    method = it.method,
    amount = Money.of(it.amount)
)}
