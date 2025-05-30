package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.coupon.application.CouponService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v3/users")
class UserCouponControllerV3(private val couponService: CouponService): UserCouponApiSpecV3 {

    @Operation(
        summary = "Redis + Kafka 기반 고성능 쿠폰 발급", 
        description = "Redis Lua Script와 Kafka를 활용한 고성능 쿠폰 발급 시스템입니다. " +
                "원자적 검증과 이벤트 기반 비동기 처리로 대용량 트래픽을 효율적으로 처리합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "쿠폰 발급 요청이 성공적으로 처리되었습니다.",
    )
    @PostMapping("/{userId}/coupons")
    override fun issueCouponWithRedisKafka(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): ResponseEntity<CouponResponse.AsyncIssue> {
        val result = couponService.issueCouponWithRedisKafka(request.toCmd(userId))
        return ResponseEntity.ok(result.toResponse())
    }
} 