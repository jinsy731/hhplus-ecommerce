package kr.hhplus.be.server.order.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.AlreadyPaidOrderException
import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.domain.model.OrderStatus
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long = 0L,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "original_total"))
    var originalTotal: Money = Money.ZERO,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discounted_amount"))
    var discountedAmount: Money = Money.ZERO,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private var _orderItems: MutableList<OrderItem> = mutableListOf(),

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @get:Transient
    val orderItems: OrderItems
        get() = OrderItems(_orderItems)


    companion object {
        fun create(context: OrderContext.Create.Root): Order {
            val order = Order(
                userId = context.userId,
                createdAt = context.timestamp,
                updatedAt = context.timestamp,
            )

            val items = OrderItem.from(context.items)
            items.forEach { order.addItem(it) }
            order.originalTotal = order.calculateOriginalTotal()

            return order
        }
    }

    fun addItem(orderItem: OrderItem) {
        this.orderItems.add(orderItem, this)
        orderItem.order = this
    }

    private fun calculateOriginalTotal(): Money {
        return this.orderItems.calculateOriginalTotal()
    }
    

    fun finalTotal(): Money = originalTotal - discountedAmount


    fun applyDiscount(discountInfos: List<DiscountInfo>) {
        this.orderItems.applyDiscounts(discountInfos)
//        discountInfos.forEach { discountLine ->
//            val orderItem = this.orderItems.find { orderItem -> orderItem.id == discountLine.orderItemId }
//            orderItem?.applyDiscount(discountLine.amount) ?: throw IllegalStateException()
//        }

        discountedAmount = discountInfos.fold(Money.ZERO) { acc, it -> acc + it.amount}
        
        // 할인액이 주문 총액을 초과하지 않도록 조정
        if (discountedAmount > originalTotal) {
            discountedAmount = originalTotal
        }
    }


    fun completeOrder() {
        if (status != OrderStatus.CREATED) {
            throw AlreadyPaidOrderException()
        }
        status = OrderStatus.PAID
    }
}
