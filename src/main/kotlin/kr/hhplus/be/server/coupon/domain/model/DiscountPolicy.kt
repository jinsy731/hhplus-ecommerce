package kr.hhplus.be.server.coupon.domain.model

import java.math.BigDecimal
import jakarta.persistence.*
import jdk.jpackage.internal.Arguments.CLIOptions.context

/**
 * 할인 정책 인터페이스
 * 할인 금액을 계산하는 메서드 제공
 */
@Entity
class DiscountPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    @Column(nullable = false)
    val name: String
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discount_type_id", nullable = false)
    val discountType: DiscountType
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discount_condition_id", nullable = false)
    val discountCondition: DiscountCondition

    constructor(name: String, discountType: DiscountType, discountCondition: DiscountCondition) {
        this.name = name
        this.discountType = discountType
        this.discountCondition = discountCondition
    }
    
    /**
     * 할인 금액 계산
     * @param price 상품 가격
     * @param context 할인 조건 검증에 필요한 컨텍스트 정보
     * @return 할인 금액 (조건을 만족하지 않으면 0)
     */
    fun calculateDiscount(price: BigDecimal): BigDecimal {
        return discountType.calculateDiscount(price)
    }
}
