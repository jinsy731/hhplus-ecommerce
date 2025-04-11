package kr.hhplus.be.server.coupon.entrypoint.http

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.common.PageInfo
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/users")
class UserCouponController: UserCouponApiSpec {

    @PostMapping("/{userId}/coupons")
    override fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: CouponRequest.Issue
    ): CommonResponse<CouponResponse.Issue> {
        val result = CouponResponse.Issue(1234, "UNUSED")
        return CommonResponse(result)
    }

    @GetMapping("/{userId}/coupons")
    override fun getCoupons(
        @PathVariable userId: Long
    ): CommonResponse<CouponResponse.Retrieve> {
        val result = CouponResponse.Retrieve(
            listOf(
                CouponResponse.UserCouponData(123, "신규가입 5천원", "FIXED", "5000", "UNUSED", LocalDateTime.of(2025, 5, 1, 10, 0, 0))
            ),
            PageInfo(0, 20, 31, 4)
        )
        return CommonResponse(result)
    }
}
