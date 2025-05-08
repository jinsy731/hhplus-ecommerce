package kr.hhplus.be.server.shared.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액은 0보다 커야합니다."),

    INVALID_COUPON_STATUS(HttpStatus.CONFLICT, "CP001", "유효하지 않은 쿠폰 상태입니다."),
    EXPIRED_COUPON(HttpStatus.CONFLICT, "CP002", "만료된 쿠폰입니다."),
    EXCEEDED_MAX_COUPON_LIMIT(HttpStatus.CONFLICT, "CP003", "발급 가능한 쿠폰 수량을 초과하였습니다."),
    COUPON_TARGET_NOT_FOUND(HttpStatus.CONFLICT, "CP004", "쿠폰 적용 가능한 상품이 없습니다."),
    DUPLICATE_COUPON_ISSUE(HttpStatus.CONFLICT , "CP005", "이미 발급받은 쿠폰입니다."),

    EMPTY_ORDER_ITEM(HttpStatus.BAD_REQUEST, "O001", "주문 항목이 비어있습니다."),
    ALREADY_PAID_ORDER(HttpStatus.CONFLICT, "O002", "이미 결제된 주문입니다."),
    VARIANT_UNAVAILABLE(HttpStatus.CONFLICT, "P001", "해당 옵션은 구매 불가합니다."),
    VARIANT_OUT_OF_STOCK(HttpStatus.CONFLICT, "P002", "해당 옵션의 재고가 부족합니다."),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT, "P003", "해당 상품은 구매 불가합니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "UP001", "잔여 포인트가 부족합니다."),
    ALREADY_PAID(HttpStatus.CONFLICT, "PMT001", "이미 결제되었습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "존재하지 않는 리소스입니다."),

}