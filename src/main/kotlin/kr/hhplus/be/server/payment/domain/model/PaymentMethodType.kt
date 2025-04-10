package kr.hhplus.be.server.payment.domain.model

/**
 * 결제 수단 유형을 나타내는 enum
 */
enum class PaymentMethodType(val isCashPayment: Boolean) {
    CREDIT_CARD(true), // 신용카드
    DEBIT_CARD(true),  // 체크카드
    BANK_TRANSFER(true),  // 계좌이체
    MOBILE_PAYMENT(true), // 모바일 결제
    VIRTUAL_ACCOUNT(true), // 가상계좌
    POINT(false),          // 포인트
    COUPON(false),         // 쿠폰
    DEPOSIT(false);        // 예치금
}
