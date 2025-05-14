package kr.hhplus.be.server.coupon.entrypoint.http

import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.coupon.application.CouponService
import org.springframework.data.domain.Pageable
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

    @GetMapping("/{userId}/coupons")
    override fun retrieveLists(
        @PathVariable userId: Long,
        pageable: Pageable,
    ): CommonResponse<CouponResponse.RetrieveLists> {
        val result = couponService.retrieveLists(userId, pageable)
        return CommonResponse(result.toResponse())
    }
}
