package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.common.PageInfo
import java.time.LocalDateTime

class CouponResponse {
    @Schema(description = "발급된 쿠폰 정보")
    data class Issue(
        @Schema(description = "유저 쿠폰 ID", example = "1234")
        val userCouponId: Long,

        @Schema(description = "상태", example = "UNUSED")
        val status: String
    )

    @Schema(description = "보유 쿠폰 목록 응답")
    data class Retrieve(
        @Schema(description = "보유 쿠폰 리스트")
        val coupons: List<UserCouponData>,

        @Schema(description = "페이지 정보")
        val pageInfo: PageInfo
    )


    @Schema(description = "보유 쿠폰 정보")
    data class UserCouponData(
        @Schema(description = "유저 쿠폰 ID", example = "123")
        val userCouponId: Long,

        @Schema(description = "쿠폰 이름", example = "신규가입 5천원 할인")
        val couponName: String,

        @Schema(description = "할인 타입 (FIXED, RATE)", example = "FIXED")
        val discountType: String,

        @Schema(description = "할인 값 (금액 또는 비율)", example = "5000")
        val value: String,

        @Schema(description = "쿠폰 상태 (UNUSED, USED, EXPIRED)", example = "UNUSED")
        val status: String,

        @Schema(description = "쿠폰 만료일", example = "2025-05-01`T`10:00:00")
        val expiredAt: LocalDateTime
    )
}