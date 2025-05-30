package kr.hhplus.be.server.payment.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.domain.BaseTimeEntity

/**
 * 결제 항목 상세 엔티티
 * 주문 항목별 결제 금액 및 환불 여부 추적
 */
@Entity
@Table(name = "payment_item_details")
class PaymentItemDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 해당 주문 항목 ID
    @Column(nullable = false)
    val orderItemId: Long,

    // 기존 가격
    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "original_amount"))
    val originalAmount: Money,

    // 할인된 가격
    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discounted_amount"))
    val discountedAmount: Money,

    // 환불 여부
    @Column(nullable = false)
    var refunded: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    var payment: Payment? = null
) : BaseTimeEntity() {
    companion object {
        fun create(order: PaymentContext.Prepare.OrderInfo): MutableList<PaymentItemDetail> {
            return order.items.map { PaymentItemDetail(
                orderItemId = it.id,
                originalAmount = it.subTotal,
                discountedAmount = it.discountedAmount,
            ) }.toMutableList()
        }
    }
}
