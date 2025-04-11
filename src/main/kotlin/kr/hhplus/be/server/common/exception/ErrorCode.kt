package kr.hhplus.be.server.common.exception

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
    ALREADY_PAID_ORDER(HttpStatus.CONFLICT, "O002", "이미 결제된 주문입니다."),
    VARIANT_UNAVAILABLE(HttpStatus.CONFLICT, "P001", "해당 옵션은 구매 불가합니다."),
    VARIANT_OUT_OF_STOCK(HttpStatus.CONFLICT, "P002", "해당 옵션의 재고가 부족합니다."),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT, "P003", "해당 상품은 구매 불가합니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "UP001", "잔여 포인트가 부족합니다."),
    ALREADY_PAID(HttpStatus.CONFLICT, "PMT001", "이미 결제되었습니다."),
    EXCEEDED_MAX_COUPON_LIMIT(HttpStatus.CONFLICT, "CP001", "발급 가능한 쿠폰 수량을 초과하였습니다.")

}