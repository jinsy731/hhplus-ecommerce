package kr.hhplus.be.server.payment.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.common.entity.BaseTimeEntity
import java.math.BigDecimal

/**
 * 결제 수단 엔티티
 * 결제에 사용된 결제 수단과 상세 정보를 관리
 */
@Entity
@Table(name = "payment_methods")
class PaymentMethod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 결제 수단 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: PaymentMethodType,

    // 해당 결제 수단으로 결제된 금액
    @Column(nullable = false)
    val amount: BigDecimal,

    // 결제 식별 정보 (카드 번호 마스킹, 계좌 정보 등)
    val identifier: String? = null,

    // 결제 메타 정보 (승인 번호, 거래 ID 등)
    val metadata: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    var payment: Payment? = null
) : BaseTimeEntity() {

    /** 현금성 결제 수단인지 여부 반환 */
    fun isCashPayment(): Boolean {
        return type.isCashPayment
    }
}