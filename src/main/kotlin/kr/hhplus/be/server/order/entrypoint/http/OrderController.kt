package kr.hhplus.be.server.order.entrypoint.http

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.order.facade.OrderFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val orderFacade: OrderFacade): OrderApiSpec {

    @PostMapping
    override fun createOrder(
        @RequestBody request: OrderRequest.Create.Root
    ): ResponseEntity<CommonResponse<OrderResponse.Create>> {
        val result = orderFacade.placeOrder(request.toCreateCriteria())
        return ResponseEntity.ok(CommonResponse(result.toCreateResponse()))
    }
}
