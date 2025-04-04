package kr.hhplus.be.server.order

import kr.hhplus.be.server.common.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    @PostMapping
    fun createOrder(
        @RequestBody request: CreateOrderRequest
    ): ApiResponse<CreateOrderResponse> {
        val result = CreateOrderResponse(999, "PAID", 15000)
        return ApiResponse("SUCCESS", "주문이 완료되었습니다.", result)
    }
}
