package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.web.ErrorResponse
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v2/users")
@Tag(name = "User Coupon V2", description = "비동기 쿠폰 발급 및 상태 조회")
interface UserCouponApiSpecV2 {

    @Operation(
        summary = "비동기 쿠폰 발급",
        description = "KVStore를 활용한 비동기 쿠폰 발급 시스템입니다. 중복 검증과 재고 확인 후 큐에 발급 요청을 추가합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "발급 요청 성공", content = [Content(schema = Schema(implementation = CouponResponse.AsyncIssue::class))]),
            ApiResponse(responseCode = "400", description = "잔여 수량 부족/중복 발급", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "사용자 또는 쿠폰 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/{userId}/coupons")
    fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): CommonResponse<CouponResponse.AsyncIssue>

    @Operation(
        summary = "비동기 쿠폰 발급 상태 조회",
        description = "비동기 방식으로 요청한 쿠폰 발급의 현재 상태를 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = CouponResponse.AsyncIssueStatus::class))]),
            ApiResponse(responseCode = "404", description = "사용자 또는 쿠폰 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @GetMapping("/{userId}/coupons/{couponId}/status")
    fun getAsyncIssueStatus(
        @PathVariable userId: Long,
        @PathVariable couponId: Long
    ): CommonResponse<CouponResponse.AsyncIssueStatus>
} 