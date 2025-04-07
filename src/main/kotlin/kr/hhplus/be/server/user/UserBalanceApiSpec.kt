package kr.hhplus.be.server.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.common.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/users")
@Tag(name = "User Balance", description = "잔액 관련 API")
interface UserBalanceApiSpec {

    @Operation(
        summary = "잔액 충전",
        description = "사용자에게 금액을 충전합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "충전 성공", content = [Content(schema = Schema(implementation = BalanceResponse::class))]),
            ApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "사용자 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/{userId}/balance")
    fun chargeBalance(
        @PathVariable userId: Long,
        @RequestBody request: ChargeBalanceRequest
    ): ResponseEntity<CommonResponse<BalanceResponse>>

    @Operation(
        summary = "잔액 조회",
        description = "사용자의 현재 잔액을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = BalanceResponse::class))]),
            ApiResponse(responseCode = "404", description = "사용자 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @GetMapping("/{userId}/balance")
    fun getBalance(
        @PathVariable userId: Long
    ): ResponseEntity<CommonResponse<BalanceResponse>>
}
