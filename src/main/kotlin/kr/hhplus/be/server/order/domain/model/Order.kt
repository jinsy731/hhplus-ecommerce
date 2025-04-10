package kr.hhplus.be.server.order.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.order.domain.EmptyOrderItemException
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED,

    @Column(nullable = false)
    var originalTotal: BigDecimal,

    @Column(nullable = false)
    var discountedAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    
    /**
     * 최종 주문 금액 계산
     */
    fun finalTotal(): BigDecimal = originalTotal - discountedAmount
    
    /**
     * 주문 항목들의 소계 합산하여 주문 총액 설정
     */
    fun calculateTotal(items: List<OrderItem>) {
        if (items.isEmpty()) {
            throw EmptyOrderItemException()
        }
        originalTotal = items.sumOf { it.subTotal() }
    }
    
    /**
     * 할인 적용
     */
    fun applyDiscount(discountLines: List<DiscountLine>) {
        discountedAmount = discountLines.sumOf { it.amount }
        
        // 할인액이 주문 총액을 초과하지 않도록 조정
        if (discountedAmount > originalTotal) {
            discountedAmount = originalTotal
        }
    }
    
    /**
     * 결제 완료 처리
     */
    fun completeOrder() {
        if (status != OrderStatus.CREATED) {
            throw IllegalStateException("이미 처리된 주문입니다: $status")
        }
        status = OrderStatus.PAID
    }
    
    /**
     * 현재 주문 상태 계산
     * 연관된 OrderItem의 상태를 기반으로 주문 상태를 업데이트합니다.
     */
    fun calculateStatus(items: List<OrderItem>) {
        // 모든 항목이 취소되었으면 주문 취소
        if (items.all { it.status == OrderItemStatus.CANCELED }) {
            status = OrderStatus.CANCELED
            return
        }
        
        // 일부 항목이 환불되었다면 환불 상태로 마킹
        if (items.any { it.status == OrderItemStatus.REFUNDED }) {
            status = OrderStatus.REFUNDED
            return
        }
        
        // 모든 항목이 배송 완료되었으면 주문 배송 완료
        if (items.all { it.status == OrderItemStatus.DELIVERED }) {
            status = OrderStatus.DELIVERED
            return
        }
        
        // 하나라도 배송 중이면 주문 배송 중
        if (items.any { it.status == OrderItemStatus.SHIPPING }) {
            status = OrderStatus.SHIPPING
            return
        }
    }
}
