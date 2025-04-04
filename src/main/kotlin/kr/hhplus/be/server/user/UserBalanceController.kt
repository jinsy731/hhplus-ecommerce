package kr.hhplus.be.server.user

import kr.hhplus.be.server.common.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserBalanceController {

    @PostMapping("/{userId}/balance")
    fun chargeBalance(
        @PathVariable userId: Long,
        @RequestBody request: ChargeBalanceRequest
    ): ApiResponse<BalanceResponse> {
        val result = BalanceResponse(userId, 15000)
        return ApiResponse("SUCCESS", "잔액이 충전되었습니다.", result)
    }

    @GetMapping("/{userId}/balance")
    fun getBalance(
        @PathVariable userId: Long
    ): ApiResponse<BalanceResponse> {
        val result = BalanceResponse(userId, 12000)
        return ApiResponse("SUCCESS", "잔액 조회 성공", result)
    }
}
