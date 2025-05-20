package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.coupon.application.dto.CouponCommand

class CouponRequest {
    @Schema(description = "쿠폰 발급 요청")
    data class Issue(
        @Schema(description = "쿠폰 ID", example = "12")
        val couponId: Long
    )
}

fun CouponRequest.Issue.toCmd(userId: Long) = CouponCommand.Issue(
    userId = userId,
    couponId = couponId
)