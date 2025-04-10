package kr.hhplus.be.server.common

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액은 0보다 커야합니다."),

    INvVALID_COUPON_STATUS(HttpStatus.CONFLICT, "C001", "유효하지 않은 쿠폰 상태입니다."),
    EXPIRED_COUPON(HttpStatus.CONFLICT, "C002", "만료된 쿠폰입니다.")
}