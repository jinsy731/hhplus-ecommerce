package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.order.domain.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 할인 유형
 */
enum class DiscountMethod {
    COUPON,
    PROMOTION,
}

@Entity
@Table(name = "discount_lines")
class DiscountLine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val orderItemId: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: DiscountMethod,

    @Column
    val sourceId: Long?,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(
            now: LocalDateTime,
            sourceId: Long,
            discountMethod: DiscountMethod,
            orderItemsDiscountMap: Map<OrderItem, BigDecimal>): List<DiscountLine> {
            return orderItemsDiscountMap.map { DiscountLine(
                orderItemId = it.key.id,
                type = discountMethod,
                sourceId = sourceId,
                amount = it.value,
                createdAt = now,
            ) }
        }
    }
}