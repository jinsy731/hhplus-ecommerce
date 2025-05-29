package kr.hhplus.be.server.order.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.web.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "주문 및 결제")
interface OrderApiSpec {

    @Operation(
        summary = "주문서 생성",
        description = "상품과 수량, 쿠폰을 입력받아 주문서를 생성합니다. 결제는 별도로 진행해야 합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "주문서 생성 성공", content = [Content(schema = Schema(implementation = OrderResponse.CreateOrderSheet::class))]),
            ApiResponse(responseCode = "400", description = "쿠폰 만료, 재고 부족", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/sheet")
    fun createOrderSheet(
        @RequestBody request: OrderRequest.CreateOrderSheet.Root
    ): ResponseEntity<CommonResponse<OrderResponse.CreateOrderSheet>>

    @Operation(
        summary = "결제 처리",
        description = "주문서 ID를 받아서 외부 PG를 통해 결제를 처리합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "결제 성공", content = [Content(schema = Schema(implementation = OrderResponse.ProcessPayment::class))]),
            ApiResponse(responseCode = "400", description = "잔액 부족, 이미 결제된 주문", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/{orderId}/payment")
    fun processPayment(
        @PathVariable orderId: Long,
        @RequestBody request: OrderRequest.ProcessPayment.Root
    ): ResponseEntity<CommonResponse<OrderResponse.ProcessPayment>>

    @Operation(
        summary = "주문 생성 (기존 API)",
        description = "상품과 수량, 쿠폰을 입력받아 주문을 생성합니다. (기존 방식 - 호환성 유지)",
        responses = [
            ApiResponse(responseCode = "200", description = "주문 성공", content = [Content(schema = Schema(implementation = OrderResponse.Create::class))]),
            ApiResponse(responseCode = "400", description = "잔액 부족, 쿠폰 만료, 재고 부족", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping
    fun createOrder(
        @RequestBody request: OrderRequest.Create.Root
    ): ResponseEntity<CommonResponse<OrderResponse.Create>>
}
