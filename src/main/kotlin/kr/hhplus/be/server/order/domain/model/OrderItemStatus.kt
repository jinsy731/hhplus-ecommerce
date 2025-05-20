package kr.hhplus.be.server.order.domain.model

/**
 * 주문 항목 상태
 */
enum class OrderItemStatus {
    /**
     * 주문됨
     */
    ORDERED,
    
    /**
     * 배송 중
     */
    SHIPPING,
    
    /**
     * 배송 완료
     */
    DELIVERED,
    
    /**
     * 취소됨
     */
    CANCELED,
    
    /**
     * 환불됨
     */
    REFUNDED
}
