package kr.hhplus.be.server.order.entrypoint.http

import kr.hhplus.be.server.order.facade.OrderFacade
import kr.hhplus.be.server.shared.web.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val orderFacade: OrderFacade): OrderApiSpec {

    @PostMapping("/sheet")
    override fun createOrderSheet(
        @RequestBody request: OrderRequest.CreateOrderSheet.Root
    ): ResponseEntity<CommonResponse<OrderResponse.CreateOrderSheet>> {
        val result = orderFacade.createOrderSheet(request.toCreateOrderSheetCriteria())
        return ResponseEntity.ok(CommonResponse(result.toCreateOrderSheetResponse()))
    }

    @PostMapping("/{orderId}/payment")
    override fun processPayment(
        @PathVariable orderId: Long,
        @RequestBody request: OrderRequest.ProcessPayment.Root
    ): ResponseEntity<CommonResponse<OrderResponse.ProcessPayment>> {
        val result = orderFacade.processPayment(request.toProcessPaymentCriteria(orderId))
        return ResponseEntity.ok(CommonResponse(result.toProcessPaymentResponse(request.pgPaymentId)))
    }

    @PostMapping
    override fun createOrder(
        @RequestBody request: OrderRequest.Create.Root
    ): ResponseEntity<CommonResponse<OrderResponse.Create>> {
        val result = orderFacade.placeOrder(request.toCreateCriteria())
        return ResponseEntity.ok(CommonResponse(result.toCreateResponse()))
    }
}
