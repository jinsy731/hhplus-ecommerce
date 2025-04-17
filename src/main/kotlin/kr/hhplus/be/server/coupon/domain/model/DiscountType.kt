package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

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
    abstract fun calculateDiscount(context: DiscountContext.Root): Map<DiscountContext.Item, BigDecimal>
}

/**
 * 정액 할인 방식 (전체 할인)
 * @param amount 할인 금액
 */
@Entity
@DiscriminatorValue("FIXED_AMOUNT_TOTAL")
class FixedAmountTotalDiscountType(
    @Column(name = "discount_amount")
    val amount: BigDecimal
) : DiscountType() {
    
    override fun calculateDiscount(context: DiscountContext.Root): Map<DiscountContext.Item, BigDecimal> {
        val totalAmount = context.items.sumOf { it.subTotal }
        val totalDiscount = amount
        val adjustedTotalDiscount = when {
            totalDiscount > totalAmount -> totalAmount
            else -> totalDiscount
        }

        return calculateDiscountsWithCorrection(totalAmount, adjustedTotalDiscount, context.items)
    }
}


/**
 * 정액 할인 방식 (항목별 할인)
 * @param amount 할인 금액
 */
@Entity
@DiscriminatorValue("FIXED_AMOUNT_PER_ITEM")
class FixedAmountPerItemDiscountType(
    @Column(name = "discount_amount")
    val amount: BigDecimal
) : DiscountType() {

    override fun calculateDiscount(context: DiscountContext.Root): Map<DiscountContext.Item, BigDecimal> {
        return context.items.associateWith {
            when {
                amount > it.subTotal -> it.subTotal
                else -> amount
            }
        }
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
    
    override fun calculateDiscount(context: DiscountContext.Root): Map<DiscountContext.Item, BigDecimal> {
        val totalAmount = context.items.sumOf { it.subTotal }
        val totalDiscount = totalAmount * rate
        val adjustedTotalDiscount = maxDiscountAmount?.let {
            if(totalDiscount > it) it else totalDiscount
        } ?: totalDiscount

        return context.items.associateWith {
            adjustedTotalDiscount / context.items.size.toBigDecimal()
        }
    }
}

fun calculateDiscountsWithCorrection(
    totalAmount: BigDecimal, // 전체 금액
    adjustedTotalDiscount: BigDecimal, // 총 할인액
    items: List<DiscountContext.Item> // 각 주문 아이템
): Map<DiscountContext.Item, BigDecimal> {

    // 아이템별 비례 할인 금액 계산
    val initialDiscounts = items.associateWith { item ->
        val ratio = item.subTotal.divide(totalAmount, 10, RoundingMode.HALF_UP)
        ratio.multiply(adjustedTotalDiscount).setScale(2, RoundingMode.HALF_UP)
    }

    // 총 할인 금액 계산
    val totalCalculatedDiscount = initialDiscounts.values.reduce { acc, d -> acc + d }

    // 차이 계산
    val diff = adjustedTotalDiscount.subtract(totalCalculatedDiscount)

    // 보정할 대상 아이템 선택 (여기서는 첫 번째 아이템으로 보정)
    val firstItem = items.firstOrNull()

    // 보정 적용 (차이를 첫 번째 아이템에 적용)
    return if (firstItem != null && diff.compareTo(BigDecimal.ZERO) != 0) {
        initialDiscounts.mapValues { (item, discount) ->
            if (item == firstItem) discount.add(diff) else discount
        }
    } else {
        initialDiscounts
    }
}