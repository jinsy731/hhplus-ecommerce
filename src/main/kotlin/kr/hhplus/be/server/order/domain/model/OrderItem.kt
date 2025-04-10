package kr.hhplus.be.server.order.domain.model

import kr.hhplus.be.server.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    
    @Column(nullable = false)
    val orderId: Long,
    
    @Column(nullable = false)
    val productId: Long,
    
    @Column(nullable = false)
    val variantId: Long,
    
    @Column(nullable = false)
    val quantity: BigDecimal,
    
    @Column(nullable = false)
    val unitPrice: BigDecimal,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderItemStatus = OrderItemStatus.ORDERED
) {
    
    /**
     * 항목의 소계를 계산합니다.
     */
    fun subTotal(): BigDecimal = unitPrice * quantity
}
