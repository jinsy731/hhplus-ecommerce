package kr.hhplus.be.server.order.domain.model

/**
 * 주문 상태
 */
enum class OrderStatus {
    /**
     * 주문 생성됨
     */
    CREATED,

    /**
     * 결제 완료
     */
    PAID,

    /**
     * 기타 실패
     */
    FAILED,

    /**
     * 배송 중
     */
    SHIPPING,

    /**
     * 배송 완료
     */
    DELIVERED,

    /**
     * 주문 취소됨
     */
    CANCELED,

    /**
     * 환불 처리됨
     */
    REFUNDED
}