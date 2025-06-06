package kr.hhplus.be.server.payment.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.BaseTimeEntity
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.AlreadyPaidException
import kr.hhplus.be.server.shared.exception.InvalidPaymentStatusException
import java.time.LocalDateTime

/**
 * 결제 상태를 나타내는 enum
 */
enum class PaymentStatus {
    PENDING,   // 결제 대기중
    PAID,      // 결제 완료
    PARTIALLY_REFUNDED,  // 부분 환불됨
    REFUNDED,  // 전액 환불됨
    FAILED     // 결제 실패
}


/**
 * 결제 엔티티
 * 주문에 대한 실제 결제 정보를 관리
 */
@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val orderId: Long,

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

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    // 결제 완료 시각
    var timestamp: LocalDateTime? = null,

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val details: MutableList<PaymentItemDetail> = mutableListOf(),
) : BaseTimeEntity() {

    companion object {
        fun create(context: PaymentContext.Prepare.Root): Payment {
            return Payment(
                orderId = context.order.id,
                originalAmount = context.order.originalTotal,
                discountedAmount = context.order.discountedAmount,
                timestamp = context.timestamp,
                details = PaymentItemDetail.create(context.order),
            )
        }
    }

    fun complete() {
        check(this.status == PaymentStatus.PENDING || this.status == PaymentStatus.FAILED) { throw AlreadyPaidException() }
        this.status = PaymentStatus.PAID
    }

    fun cancel() {
        check(this.status == PaymentStatus.PAID) { throw InvalidPaymentStatusException() }
        this.status = PaymentStatus.REFUNDED
    }

    fun fail() {
        check(this.status == PaymentStatus.PENDING) { throw InvalidPaymentStatusException() }
        this.status = PaymentStatus.FAILED
    }
}
