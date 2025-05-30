package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.web.ErrorResponse
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/users")
@Tag(name = "User Coupon V1", description = "동기 쿠폰 발급 및 보유 쿠폰 조회")
interface UserCouponApiSpecV1 {

    @Operation(
        summary = "동기 쿠폰 발급",
        description = "사용자에게 쿠폰을 동기적으로 발급합니다. 분산락을 사용한 안전한 발급 처리입니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "발급 성공", content = [Content(schema = Schema(implementation = CouponResponse.Issue::class))]),
            ApiResponse(responseCode = "400", description = "잔여 수량 부족/중복 발급", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "사용자 또는 쿠폰 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/{userId}/coupons")
    fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): CommonResponse<CouponResponse.Issue>

    @Operation(
        summary = "보유 쿠폰 목록 조회",
        description = "사용자가 보유한 쿠폰 목록을 페이징으로 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = CouponResponse.RetrieveLists::class))]),
            ApiResponse(responseCode = "404", description = "사용자 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @GetMapping("/{userId}/coupons")
    fun retrieveLists(
        @PathVariable userId: Long,
        pageable: Pageable,
    ): CommonResponse<CouponResponse.RetrieveLists>
} 