package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderStatus
import java.math.BigDecimal

class OrderResponse {
    @Schema(description = "주문 생성 응답")
    data class Create(
        @Schema(description = "주문 ID", example = "999")
        val orderId: Long,

        @Schema(description = "주문 상태", example = "PAID")
        val status: OrderStatus,

        @Schema(description = "최종 결제 금액", example = "15000")
        val finalTotal: BigDecimal
    )
}

fun Order.toCreateResponse() = OrderResponse.Create(
    orderId = this.id,
    status = this.status,
    finalTotal = this.finalTotal()
)