package kr.hhplus.be.server.order.entrypoint.http

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.order.domain.OrderStatus
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
        // 임시 응답 (실제 구현은 entrypoint.http 패키지의 클래스에서 확인)
        val result = CreateOrderResponse(999, OrderStatus.PAID, 15000)
        return CommonResponse(result)
    }
}
