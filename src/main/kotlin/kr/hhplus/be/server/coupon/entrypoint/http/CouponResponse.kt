package kr.hhplus.be.server.coupon.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.common.PageInfo
import kr.hhplus.be.server.common.toResponse
import kr.hhplus.be.server.coupon.application.CouponResult
import java.time.LocalDateTime

class CouponResponse {
    @Schema(description = "발급된 쿠폰 정보")
    data class Issue(
        @Schema(description = "유저 쿠폰 ID", example = "1234")
        val userCouponId: Long,

        @Schema(description = "상태", example = "UNUSED")
        val status: String,

        @Schema(description = "만료일시", example = "2025-05-01T10:00:00")
        val expiredAt: LocalDateTime
    )

    @Schema(description = "보유 쿠폰 목록 응답")
    data class RetrieveLists(
        @Schema(description = "보유 쿠폰 리스트")
        val coupons: List<UserCouponData>,

        @Schema(description = "페이지 정보")
        val pageInfo: PageInfo
    )


    @Schema(description = "보유 쿠폰 정보")
    data class UserCouponData(
        @Schema(description = "유저 쿠폰 ID", example = "123")
        val id: Long,

        @Schema(description = "쿠폰 ID", example = "123")
        val couponId: Long,

        @Schema(description = "쿠폰 이름", example = "신규가입 5천원 할인")
        val couponName: String,

        @Schema(description = "쿠폰 설명", example = "신규가입 5천원 할인")
        val description: String,

        @Schema(description = "할인 정책 이름", example = "주문 상품 전체 할인 5000원")
        val discountPolicyName: String,

        @Schema(description = "할인 값 (금액 또는 비율)", example = "5000")
        val value: Number?,

        @Schema(description = "쿠폰 상태 (UNUSED, USED, EXPIRED)", example = "UNUSED")
        val status: String,

        @Schema(description = "쿠폰 만료일", example = "2025-05-01`T`10:00:00")
        val expiredAt: LocalDateTime
    )
}

fun CouponResult.Issue.toResponse() = CouponResponse.Issue(
    userCouponId = this.userCouponId!!,
    status = this.status.name,
    expiredAt = this.expiredAt
)

fun CouponResult.RetrieveList.toResponse() = CouponResponse.RetrieveLists(
    coupons = this.coupons.map { it.toResponse() },
    pageInfo = this.pageResult.toResponse()
)

fun CouponResult.UserCouponData.toResponse() = CouponResponse.UserCouponData(
    id = this.id,
    couponId = this.couponId,
    couponName = this.couponName,
    description = this.description,
    discountPolicyName = this.discountPolicyName,
    value = this.value,
    status = this.status,
    expiredAt = this.expiredAt
)