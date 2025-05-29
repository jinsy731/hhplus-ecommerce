package kr.hhplus.be.server.payment

import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentContext
import kr.hhplus.be.server.payment.domain.model.PaymentItemDetail
import kr.hhplus.be.server.payment.domain.model.PaymentStatus
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * 결제 관련 테스트 픽스처
 * 테스트 의도에 맞게 결제 객체를 생성합니다.
 */
object PaymentTestFixture {
    
    // 기본 결제 생성 빌더
    fun payment(
        id: Long = 0L,
        orderId: Long = 1L,
        originalAmount: Money = Money.of(20000),
        discountedAmount: Money = Money.ZERO,
        status: PaymentStatus = PaymentStatus.PENDING,
        timestamp: LocalDateTime? = null
    ): PaymentBuilder {
        return PaymentBuilder(
            id = id,
            orderId = orderId,
            originalAmount = originalAmount,
            discountedAmount = discountedAmount,
            status = status,
            timestamp = timestamp
        )
    }
    
    // 결제 컨텍스트 생성 빌더
    fun paymentContext(
        orderId: Long = 1L,
        userId: Long = 1L,
        originalTotal: Money = Money.of(20000),
        discountedAmount: Money = Money.ZERO,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): PaymentContextBuilder {
        return PaymentContextBuilder(
            orderId = orderId,
            userId = userId,
            originalTotal = originalTotal,
            discountedAmount = discountedAmount,
            timestamp = timestamp
        )
    }
    
    // 결제 항목 상세 생성
    fun paymentItemDetail(
        id: Long = 0L,
        orderItemId: Long = 1L,
        originalAmount: Money = Money.of(10000),
        discountedAmount: Money = Money.ZERO,
        refunded: Boolean = false
    ): PaymentItemDetail {
        return PaymentItemDetail(
            id = id,
            orderItemId = orderItemId,
            originalAmount = originalAmount,
            discountedAmount = discountedAmount,
            refunded = refunded
        )
    }
    
    // 시나리오별 결제 생성 메서드
    
    /**
     * 결제 완료된 결제 생성
     */
    fun completedPayment(
        orderId: Long = 1L,
        amount: Money = Money.of(20000),
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Payment {
        return payment(
            orderId = orderId,
            originalAmount = amount,
            status = PaymentStatus.PAID,
            timestamp = timestamp
        ).build()
    }
    
    /**
     * 결제 대기중인 결제 생성
     */
    fun pendingPayment(
        orderId: Long = 1L,
        amount: Money = Money.of(20000)
    ): Payment {
        return payment(
            orderId = orderId,
            originalAmount = amount,
            status = PaymentStatus.PENDING
        ).build()
    }
    
    /**
     * 실패한 결제 생성
     */
    fun failedPayment(
        orderId: Long = 1L,
        amount: Money = Money.of(20000)
    ): Payment {
        return payment(
            orderId = orderId,
            originalAmount = amount,
            status = PaymentStatus.FAILED
        ).build()
    }
    
    /**
     * 할인이 적용된 결제 생성
     */
    fun discountedPayment(
        orderId: Long = 1L,
        originalAmount: Money = Money.of(20000),
        discountedAmount: Money = Money.of(2000)
    ): Payment {
        return payment(
            orderId = orderId,
            originalAmount = originalAmount,
            discountedAmount = discountedAmount,
            status = PaymentStatus.PAID
        ).build()
    }
    
    // 빌더 클래스들
    class PaymentBuilder(
        private val id: Long,
        private val orderId: Long,
        private val originalAmount: Money,
        private val discountedAmount: Money,
        private val status: PaymentStatus,
        private val timestamp: LocalDateTime?
    ) {
        private val details: MutableList<PaymentItemDetail> = mutableListOf()
        
        fun build(): Payment {
            val payment = Payment(
                id = id,
                orderId = orderId,
                originalAmount = originalAmount,
                discountedAmount = discountedAmount,
                status = status,
                timestamp = timestamp
            )
            
            details.forEach { 
                it.payment = payment
                payment.details.add(it)
            }
            
            return payment
        }
        
        fun withStatus(status: PaymentStatus): PaymentBuilder {
            return PaymentBuilder(
                id = id,
                orderId = orderId,
                originalAmount = originalAmount,
                discountedAmount = discountedAmount,
                status = status,
                timestamp = timestamp
            )
        }
        
        fun withDetails(vararg details: PaymentItemDetail): PaymentBuilder {
            this.details.addAll(details)
            return this
        }
        
        fun withTimestamp(timestamp: LocalDateTime): PaymentBuilder {
            return PaymentBuilder(
                id = id,
                orderId = orderId,
                originalAmount = originalAmount,
                discountedAmount = discountedAmount,
                status = status,
                timestamp = timestamp
            )
        }
    }
    
    class PaymentContextBuilder(
        private val orderId: Long,
        private val userId: Long,
        private val originalTotal: Money,
        private val discountedAmount: Money,
        private val timestamp: LocalDateTime
    ) {
        private val items: MutableList<PaymentContext.Prepare.OrderItemInfo> = mutableListOf()
        
        fun build(): PaymentContext.Prepare.Root {
            val orderInfo = PaymentContext.Prepare.OrderInfo(
                id = orderId,
                userId = userId,
                items = if (items.isEmpty()) {
                    listOf(
                        PaymentContext.Prepare.OrderItemInfo(
                            id = 1L,
                            productId = 1L,
                            variantId = 1L,
                            subTotal = Money.of(10000),
                            discountedAmount = Money.ZERO
                        ),
                        PaymentContext.Prepare.OrderItemInfo(
                            id = 2L,
                            productId = 1L,
                            variantId = 2L,
                            subTotal = Money.of(10000),
                            discountedAmount = Money.ZERO
                        )
                    )
                } else {
                    items
                },
                originalTotal = originalTotal,
                discountedAmount = discountedAmount
            )
            
            return PaymentContext.Prepare.Root(
                order = orderInfo,
                timestamp = timestamp
            )
        }
        
        fun withItems(vararg items: PaymentContext.Prepare.OrderItemInfo): PaymentContextBuilder {
            this.items.addAll(items)
            return this
        }
        
        fun withItem(
            id: Long = 1L,
            productId: Long = 1L,
            variantId: Long = 1L,
            subTotal: Money = Money.of(10000),
            discountedAmount: Money = Money.ZERO
        ): PaymentContextBuilder {
            this.items.add(
                PaymentContext.Prepare.OrderItemInfo(
                    id = id,
                    productId = productId,
                    variantId = variantId,
                    subTotal = subTotal,
                    discountedAmount = discountedAmount
                )
            )
            return this
        }
    }
} 