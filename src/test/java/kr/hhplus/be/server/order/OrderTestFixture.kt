package kr.hhplus.be.server.order

import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import kr.hhplus.be.server.order.domain.model.OrderItemStatus
import kr.hhplus.be.server.order.domain.model.OrderStatus
import java.time.LocalDateTime

/**
 * 주문 관련 테스트 픽스처
 * 빌더 패턴을 활용하여 테스트에 필요한 주문 객체를 생성합니다.
 */
object OrderTestFixture {
    
    // 기본 주문 생성 (테스트 의도에 맞게 커스터마이징 가능)
    fun order(
        id: Long = 1L,
        userId: Long = 1L,
        status: OrderStatus = OrderStatus.CREATED,
        originalTotal: Money = Money.of(10000),
        discountedAmount: Money = Money.ZERO,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): OrderBuilder {
        return OrderBuilder(
            id = id,
            userId = userId,
            status = status,
            originalTotal = originalTotal,
            discountedAmount = discountedAmount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    // 시나리오별 팩토리 메서드
    
    /**
     * 할인이 적용된 주문 생성
     * - 총액 10000원
     * - 할인금액 9000원
     * - 총 2개의 상품 주문
     * - 각 상품은 1000원 * 5개 = 5000원에서 500원 할인 적용됨
     */
    fun discountedOrder(userId: Long = 1L): Order {
        return order(userId = userId)
            .withOriginalTotal(Money.of(10000))
            .withDiscountedAmount(Money.of(9000))
            .withDiscountedItems()
            .build()
    }

    /**
     * 할인이 없는 일반 주문 생성
     * - 총액 10000원
     * - 할인금액 0원
     * - 총 2개의 상품 주문
     * - 각 상품은 1000원 * 5개 = 5000원
     */
    fun standardOrder(userId: Long = 1L): Order {
        return order(userId = userId)
            .withOriginalTotal(Money.of(10000))
            .withStandardItems()
            .build()
    }
    
    /**
     * 결제 완료된 주문 생성
     */
    fun paidOrder(userId: Long = 1L): Order {
        return order(userId = userId)
            .withStatus(OrderStatus.PAID)
            .withStandardItems()
            .build()
    }
    
    /**
     * 최소 주문 금액에 미달하는 주문 생성 (쿠폰 테스트용)
     */
    fun lowAmountOrder(userId: Long = 1L): Order {
        return order(userId = userId)
            .withOriginalTotal(Money.of(5000))
            .withStandardItems()
            .build()
    }
    
    // 이중 클래스를 통한 빌더 패턴 구현
    class OrderBuilder(
        private val id: Long,
        private val userId: Long,
        private var status: OrderStatus,
        private var originalTotal: Money,
        private var discountedAmount: Money,
        private val createdAt: LocalDateTime,
        private val updatedAt: LocalDateTime
    ) {
        private val items: MutableList<OrderItem> = mutableListOf()
        
        fun build(): Order {
            val order = Order(
                id = id,
                userId = userId,
                status = status,
                originalTotal = originalTotal,
                discountedAmount = discountedAmount,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
            
            items.forEach { order.addItem(it) }
            return order
        }
        
        fun withStatus(status: OrderStatus): OrderBuilder {
            this.status = status
            return this
        }
        
        fun withOriginalTotal(originalTotal: Money): OrderBuilder {
            this.originalTotal = originalTotal
            return this
        }
        
        fun withDiscountedAmount(discountedAmount: Money): OrderBuilder {
            this.discountedAmount = discountedAmount
            return this
        }
        
        fun withItem(
            id: Long = items.size.toLong() + 1,
            productId: Long = 1L,
            variantId: Long = 1L,
            quantity: Int = 5,
            unitPrice: Money = Money.of(1000),
            discountAmount: Money = Money.ZERO,
            status: OrderItemStatus = OrderItemStatus.ORDERED
        ): OrderBuilder {
            val order = Order(
                id = this.id,
                userId = this.userId,
                status = this.status,
                originalTotal = this.originalTotal,
                discountedAmount = this.discountedAmount,
                createdAt = this.createdAt,
                updatedAt = this.updatedAt
            )
            
            this.items.add(
                OrderItem(
                    id = id,
                    productId = productId,
                    variantId = variantId,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    discountAmount = discountAmount,
                    status = status,
                    order = order
                )
            )
            
            return this
        }
        
        fun withStandardItems(): OrderBuilder {
            return withItem(id = 1L, discountAmount = Money.ZERO)
                .withItem(id = 2L, discountAmount = Money.ZERO)
        }
        
        fun withDiscountedItems(): OrderBuilder {
            return withItem(id = 1L, discountAmount = Money.of(500))
                .withItem(id = 2L, discountAmount = Money.of(500))
        }
    }

    // 기존 호환성 유지 메서드들 (리팩토링 기간 동안만 유지)
    @Deprecated("새로운 discountedOrder() 메서드를 사용하세요", ReplaceWith("discountedOrder(userId)"))
    fun createDiscountedOrder(userId: Long = 1L): Order = discountedOrder(userId)

    @Deprecated("새로운 standardOrder() 메서드를 사용하세요", ReplaceWith("standardOrder(userId)"))
    fun createOrder(userId: Long = 1L): Order = standardOrder(userId)

    @Deprecated("OrderBuilder.withStandardItems()를 사용하세요")
    fun createOrderItems(order: Order) = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.ZERO,
            status = OrderItemStatus.ORDERED,
            order = order
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.ZERO,
            status = OrderItemStatus.ORDERED,
            order = order
        ),
    )

    @Deprecated("OrderBuilder.withDiscountedItems()를 사용하세요")
    fun createDiscountedOrderItems(order: Order) = mutableListOf(
        OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.of(500),
            status = OrderItemStatus.ORDERED,
            order = order
        ),
        OrderItem(
            id = 2L,
            productId = 1L,
            variantId = 1L,
            quantity = 5,
            unitPrice = Money.of(1000),
            discountAmount = Money.of(500),
            status = OrderItemStatus.ORDERED,
            order = order
        ),
    )
}