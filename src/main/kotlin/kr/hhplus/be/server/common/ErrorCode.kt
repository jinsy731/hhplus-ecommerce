package kr.hhplus.be.server.common

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액은 0보다 커야합니다."),

    INVALID_COUPON_STATUS(HttpStatus.CONFLICT, "C001", "유효하지 않은 쿠폰 상태입니다."),
    EXPIRED_COUPON(HttpStatus.CONFLICT, "C002", "만료된 쿠폰입니다."),

    EMPTY_ORDER_ITEM(HttpStatus.BAD_REQUEST, "O001", "주문 항목이 비어있습니다."),
    ALREADY_PAID_ORDER(HttpStatus.CONFLICT, "O002", "이미 결제된 주문입니다.")
}