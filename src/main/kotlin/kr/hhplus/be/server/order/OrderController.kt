package kr.hhplus.be.server.order

import kr.hhplus.be.server.common.CommonResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController: OrderApiSpec {

    @PostMapping
    override fun createOrder(
        @RequestBody request: CreateOrderRequest
    ): CommonResponse<CreateOrderResponse> {
        val result = CreateOrderResponse(999, "PAID", 15000)
        return CommonResponse(result)
    }
}
