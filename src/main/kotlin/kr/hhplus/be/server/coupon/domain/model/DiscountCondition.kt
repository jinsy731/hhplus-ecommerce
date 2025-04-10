package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.coupon.LongSetConverter
import java.math.BigDecimal

/**
 * 할인 조건을 정의하는 인터페이스
 * 구체적인 할인 조건들은 이 인터페이스를 구현
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "condition_type")
abstract class DiscountCondition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null


    /**
     * 할인 조건 충족 여부 확인 메서드
     * @param context 할인 조건 검증에 필요한 컨텍스트 정보
     * @return 조건 충족 여부
     */
    abstract fun isSatisfiedBy(context: DiscountContext): Boolean
}

/**
 * 할인 조건 검증에 필요한 컨텍스트 정보
 */
data class DiscountContext(
    val userId: Long? = null,
    val productId: Long? = null,
    val variantId: Long? = null,
    val orderAmount: BigDecimal? = null,

    // 필요한 다른 컨텍스트 정보들을 추가할 수 있음
    val additionalContext: Map<String, Any> = emptyMap()
)

/**
 * 최소 주문 금액 조건
 * @param minAmount 최소 주문 금액
 */
@Entity
@DiscriminatorValue("MIN_ORDER_AMOUNT")
class MinOrderAmountCondition(
    @Column(name = "min_amount")
    val minAmount: BigDecimal
) : DiscountCondition() {

    override fun isSatisfiedBy(context: DiscountContext): Boolean {
        return context.orderAmount?.let { it >= minAmount } ?: false
    }
}

/**
 * 특정 상품 할인 조건
 * @param productId 상품 ID
 */
@Entity
@DiscriminatorValue("SPECIFIC_PRODUCT")
class SpecificProductCondition(
    @Column(name = "product_id") @Convert(converter = LongSetConverter::class)
    val productIds: Set<Long>
) : DiscountCondition() {
    
    override fun isSatisfiedBy(context: DiscountContext): Boolean {
        return productIds.contains(context.productId)
    }
}


/**
 * 복합 할인 조건의 논리 연산 타입
 */
enum class LogicalOperator {
    AND, OR
}

/**
 * 복합 할인 조건 인터페이스
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "composite_type")
abstract class CompositeCondition : DiscountCondition() {
    
    @ManyToMany
    @JoinTable(
        name = "composite_condition_mapping",
        joinColumns = [JoinColumn(name = "composite_condition_id")],
        inverseJoinColumns = [JoinColumn(name = "condition_id")]
    )
    val conditions: MutableList<DiscountCondition> = mutableListOf()
    
    fun addCondition(condition: DiscountCondition): CompositeCondition {
        conditions.add(condition)
        return this
    }
    
    fun removeCondition(condition: DiscountCondition): CompositeCondition {
        conditions.remove(condition)
        return this
    }
}

/**
 * AND 논리로 연결된 복합 할인 조건
 * 모든 조건이 만족되어야 true를 반환
 */
@Entity
@DiscriminatorValue("AND")
class AndCompositeCondition : CompositeCondition() {
    
    override fun isSatisfiedBy(context: DiscountContext): Boolean {
        return conditions.isNotEmpty() && conditions.all { it.isSatisfiedBy(context) }
    }
}

/**
 * OR 논리로 연결된 복합 할인 조건
 * 하나 이상의 조건이 만족되면 true를 반환
 */
@Entity
@DiscriminatorValue("OR")
class OrCompositeCondition : CompositeCondition() {
    
    override fun isSatisfiedBy(context: DiscountContext): Boolean {
        return conditions.isNotEmpty() && conditions.any { it.isSatisfiedBy(context) }
    }
}
