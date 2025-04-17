package kr.hhplus.be.server.order.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long = 0L,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val variantId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val unitPrice: BigDecimal,

    @Column(nullable = false)
    var discountAmount: BigDecimal = BigDecimal.ZERO, // 상품별 할인 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderItemStatus = OrderItemStatus.ORDERED,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null
) {
    companion object {
        fun from(itemContext: List<OrderContext.Create.Item>): MutableList<OrderItem> = itemContext.map { OrderItem(
            productId = it.productId,
            variantId = it.variantId,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
        ) }.toMutableList()
    }

    /**
     * 항목의 소계를 계산합니다.
     */
    fun subTotal(): BigDecimal = unitPrice * quantity.toBigDecimal() - discountAmount

    fun subTotalBeforeDiscount(): BigDecimal = unitPrice * quantity.toBigDecimal()

    fun applyDiscount(amount: BigDecimal) {
        this.discountAmount += amount
    }
}
