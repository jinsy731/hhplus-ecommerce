package kr.hhplus.be.server.order.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.Money

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long? = null,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val variantId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "unit_price"))
    val unitPrice: Money,

    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discount_amount"))
    var discountAmount: Money = Money.ZERO, // 상품별 할인 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderItemStatus = OrderItemStatus.ORDERED,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null
) {
    @get:Transient
    val subTotal: Money
            get() = unitPrice * quantity.toBigDecimal() - discountAmount

    companion object {
        fun from(itemContext: List<OrderContext.Create.Item>): MutableList<OrderItem> = itemContext.map { OrderItem(
            productId = it.productId,
            variantId = it.variantId,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
        ) }.toMutableList()
    }

    fun applyDiscount(amount: Money) {
        this.discountAmount += amount
    }
}
