package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.coupon.application.CouponService
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserCouponController(private val couponService: CouponService): UserCouponApiSpec {

    @PostMapping("/{userId}/coupons")
    override fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): CommonResponse<CouponResponse.Issue> {
        val result = couponService.issueCoupon(request.toCmd(userId))
        return CommonResponse(result.toResponse())
    }


    @Operation(summary = "비동기 쿠폰 발급", description = "쿠폰을 비동기적으로 발급합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "쿠폰 발급 요청이 성공적으로 접수되었습니다.",
    )
    @PostMapping("/{userId}/coupons/async")
    fun issueCouponAsync(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): ResponseEntity<CouponResponse.AsyncIssue> {
        val result = couponService.issueCouponAsync(request.toCmd(userId))
        return ResponseEntity.ok(result.toResponse())
    }

    @GetMapping("/{userId}/coupons")
    override fun retrieveLists(
        @PathVariable userId: Long,
        pageable: Pageable,
    ): CommonResponse<CouponResponse.RetrieveLists> {
        val result = couponService.retrieveLists(userId, pageable)
        return CommonResponse(result.toResponse())
    }
}
