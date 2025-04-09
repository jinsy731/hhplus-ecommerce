package kr.hhplus.be.server.coupon

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.common.PageInfo
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/users")
class UserCouponController: UserCouponApiSpec {

    @PostMapping("/{userId}/coupons")
    override fun issueCoupon(
        @PathVariable userId: Long,
        @RequestBody request: IssueCouponRequest
    ): CommonResponse<IssueCouponResponse> {
        val result = IssueCouponResponse(1234, "UNUSED")
        return CommonResponse(result)
    }

    @GetMapping("/{userId}/coupons")
    override fun getCoupons(
        @PathVariable userId: Long
    ): CommonResponse<UserCouponListResponse> {
        val result = UserCouponListResponse(
            listOf(
                UserCouponResponse(123, "신규가입 5천원", "FIXED", "5000", "UNUSED", LocalDate.of(2025, 5, 1))
            ),
            PageInfo(0, 20, 31, 4)
        )
        return CommonResponse(result)
    }
}
