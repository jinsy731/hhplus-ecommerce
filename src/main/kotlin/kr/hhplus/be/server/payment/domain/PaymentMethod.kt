package kr.hhplus.be.server.payment.domain

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
import kr.hhplus.be.server.payment.application.PaymentCommand
import java.math.BigDecimal
import kotlin.collections.map


/**
 * 결제 수단 유형을 나타내는 enum
 */
enum class PaymentMethodType {
    CREDIT_CARD, // 신용카드
    DEBIT_CARD,  // 체크카드
    BANK_TRANSFER,  // 계좌이체
    MOBILE_PAYMENT, // 모바일 결제
    VIRTUAL_ACCOUNT, // 가상계좌
    POINT,          // 포인트
    COUPON,         // 쿠폰
    DEPOSIT;        // 예치금
}


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
    companion object {
        fun create(payMethods: List<PaymentContext.Prepare.PayMethod>): MutableList<PaymentMethod> {
            return payMethods.map { PaymentMethod(
                type = it.type,
                amount = it.amount,
            ) }.toMutableList()
        }
    }
}