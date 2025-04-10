package kr.hhplus.be.server.order.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.order.domain.AlreadyPaidOrderException
import kr.hhplus.be.server.order.entrypoint.http.OrderItemRequest
import kr.hhplus.be.server.product.domain.Product
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
    var originalTotal: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var discountedAmount: BigDecimal = BigDecimal.ZERO,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var discountLines: MutableList<DiscountLine> = mutableListOf(),

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(userId: Long, products: List<Product>, orderItemRequests: List<OrderItemRequest>): Order {
            return Order(
                userId = userId,
                orderItems = createOrderItems(products, orderItemRequests)
            ).apply { this.originalTotal = calculateOriginalTotal()}
        }

        private fun createOrderItems(products: List<Product>, orderItemRequests: List<OrderItemRequest>): MutableList<OrderItem> {
            return orderItemRequests.map { orderItemReq -> with(orderItemReq) {
                val product = products.find { it.id == productId } ?: throw IllegalArgumentException("존재하지 않는 상품입니다.")
                product.checkAvailableToOrder(variantId, quantity)
                OrderItem(
                    productId = product.id!!,
                    variantId = variantId,
                    quantity = quantity,
                    unitPrice = product.getVariantPrice(variantId)
                )
            }}.toMutableList()
        }
    }

    private fun calculateOriginalTotal(): BigDecimal {
        return this.orderItems.sumOf { it.subTotal() }
    }
    
    /**
     * 최종 주문 금액 계산
     */
    fun finalTotal(): BigDecimal = originalTotal - discountedAmount

    /**
     * 할인 적용
     */
    fun applyDiscount(discountLines: List<DiscountLine>) {
        this.discountLines.addAll(discountLines)
        discountLines.forEach { it.order = this }

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
            throw AlreadyPaidOrderException()
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
