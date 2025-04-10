package kr.hhplus.be.server.payment.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity
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

    // 기존 총 금액
    @Column(nullable = false)
    val originalTotal: BigDecimal,

    // 할인 적용 후 총 금액
    @Column(nullable = false)
    val finalTotal: BigDecimal,

    // 비현금성 결제 수단으로 결제한 금액
    @Column(nullable = false)
    val nonCashAmount: BigDecimal,

    // 현금성 결제 수단으로 결제한 금액
    @Column(nullable = false)
    val paidAmount: BigDecimal,

    // 누적 환불 금액
    @Column(nullable = false)
    var refundedAmount: BigDecimal = BigDecimal.ZERO,

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


    /**
     * 상세 항목 추가
     */
    fun addDetail(detail: PaymentItemDetail) {
        details.add(detail)
        detail.payment = this
    }

    /**
     * 결제 수단 추가
     */
    fun addMethod(method: PaymentMethod) {
        methods.add(method)
        method.payment = this
    }
}
