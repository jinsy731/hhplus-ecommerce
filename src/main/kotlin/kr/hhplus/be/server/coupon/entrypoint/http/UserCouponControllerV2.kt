package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.shared.web.CommonResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v2/users")
class UserCouponControllerV2(private val couponService: CouponService): UserCouponApiSpecV2 {

    @Operation(
        summary = "비동기 쿠폰 발급", 
        description = "KVStore를 활용한 비동기 쿠폰 발급 시스템입니다. " +
                "중복 검증과 재고 확인 후 큐에 발급 요청을 추가합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "쿠폰 발급 요청이 성공적으로 처리되었습니다.",
    )
    @PostMapping("/{userId}/coupons")
    override fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): CommonResponse<CouponResponse.AsyncIssue> {
        val result = couponService.issueCouponAsync(request.toCmd(userId))
        return CommonResponse(result.toResponse())
    }

    @GetMapping("/{userId}/coupons/{couponId}/status")
    override fun getAsyncIssueStatus(
        @PathVariable userId: Long,
        @PathVariable couponId: Long
    ): CommonResponse<CouponResponse.AsyncIssueStatus> {
        val result = couponService.getIssueStatus(userId, couponId)
        return CommonResponse(result.toResponse())
    }
} 