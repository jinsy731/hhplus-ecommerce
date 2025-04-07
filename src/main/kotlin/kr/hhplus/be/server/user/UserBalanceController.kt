package kr.hhplus.be.server.user

import kr.hhplus.be.server.common.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserBalanceController : UserBalanceApiSpec {

    override fun chargeBalance(userId: Long, request: ChargeBalanceRequest): ResponseEntity<CommonResponse<BalanceResponse>> {
        val result = BalanceResponse(userId, 15000)
        return ResponseEntity.ok(CommonResponse("SUCCESS", "잔액이 충전되었습니다.", result))
    }

    override fun getBalance(userId: Long): ResponseEntity<CommonResponse<BalanceResponse>> {
        val result = BalanceResponse(userId, 12000)
        return ResponseEntity.ok(CommonResponse("SUCCESS", "잔액 조회 성공", result))
    }
}

