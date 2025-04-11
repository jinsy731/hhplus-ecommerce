package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 할인 방식을 정의하는 추상 클래스
 * 이 클래스를 상속받아 구체적인 할인 방식 구현
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
abstract class DiscountType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    /**
     * 할인 금액 계산 메서드
     * @param price 상품 가격
     * @return 할인된 금액
     */
    abstract fun calculateDiscount(price: BigDecimal): BigDecimal
}

/**
 * 정액 할인 방식
 * @param amount 할인 금액
 */
@Entity
@DiscriminatorValue("FIXED_AMOUNT")
class FixedAmountDiscountType(
    @Column(name = "discount_amount")
    val amount: BigDecimal
) : DiscountType() {
    
    override fun calculateDiscount(price: BigDecimal): BigDecimal {
        return if (price < amount) price else amount
    }
}

/**
 * 정률 할인 방식
 * @param rate 할인율 (0.0 ~ 1.0)
 * @param maxDiscountAmount 최대 할인 금액 (null인 경우 제한 없음)
 */
@Entity
@DiscriminatorValue("RATE")
class RateDiscountType(
    @Column(name = "discount_rate")
    val rate: BigDecimal,
    
    @Column(name = "max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null
) : DiscountType() {
    
    override fun calculateDiscount(price: BigDecimal): BigDecimal {
        val discountAmount = price.multiply(rate)
        return maxDiscountAmount?.let {
            if (discountAmount > it) it else discountAmount
        } ?: discountAmount
    }
}
