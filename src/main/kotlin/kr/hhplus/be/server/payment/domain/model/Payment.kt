package kr.hhplus.be.server.payment.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity
import kr.hhplus.be.server.coupon.domain.AlreadyPaidException
import kr.hhplus.be.server.order.domain.AlreadyPaidOrderException
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import java.math.BigDecimal
import java.time.LocalDateTime

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
    val originalAmount: BigDecimal,

    // 할인된 가격
    @Column(nullable = false)
    val discountedAmount: BigDecimal,

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    // 결제 완료 시각
    var timestamp: LocalDateTime? = null,

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val details: MutableList<PaymentItemDetail> = mutableListOf(),

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val methods: MutableList<PaymentMethod> = mutableListOf()
) : BaseTimeEntity() {

    companion object {
        fun create(cmd: PaymentCommand.Prepare): Payment {
            return Payment(
                orderId = cmd.order.id,
                originalAmount = cmd.order.originalTotal,
                discountedAmount = cmd.order.discountedAmount,
                timestamp = cmd.now,
                details = createPaymentDetails(cmd.order),
                methods = createPaymentMethods(cmd.payMethods)
            )
        }

        private fun createPaymentDetails(order: Order): MutableList<PaymentItemDetail> {
            return order.orderItems.map { PaymentItemDetail(
                orderItemId = it.id,
                originalAmount = it.subTotalBeforeDiscount(),
                discountedAmount = it.discountAmount,
            ) }.toMutableList()
        }

        private fun createPaymentMethods(payMethods: List<PaymentCommand.PayMethod>): MutableList<PaymentMethod> {
            return payMethods.map { PaymentMethod(
                type = it.type,
                amount = it.amount,
            ) }.toMutableList()
        }
    }

    fun completePayment() {
        check(this.status == PaymentStatus.PENDING || this.status == PaymentStatus.FAILED) { throw AlreadyPaidException() }
        this.status = PaymentStatus.PAID
    }
}
