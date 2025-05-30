package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.shared.web.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v3/users")
@Tag(name = "User Coupon V3", description = "Redis + Kafka 기반 고성능 쿠폰 발급")
interface UserCouponApiSpecV3 {

    @Operation(
        summary = "Redis + Kafka 기반 고성능 쿠폰 발급",
        description = "Redis Lua Script와 Kafka를 활용한 고성능 쿠폰 발급 시스템입니다. " +
                "원자적 검증과 이벤트 기반 비동기 처리로 대용량 트래픽을 효율적으로 처리합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "쿠폰 발급 요청이 성공적으로 처리되었습니다.", content = [Content(schema = Schema(implementation = CouponResponse.AsyncIssue::class))]),
            ApiResponse(responseCode = "400", description = "잔여 수량 부족/중복 발급", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "사용자 또는 쿠폰 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    @PostMapping("/{userId}/coupons")
    fun issueCouponWithRedisKafka(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): ResponseEntity<CouponResponse.AsyncIssue>
} 
 