package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime
import kotlin.collections.map

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
    val sourceId: Long,

    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "amount"))
    val amount: Money,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(
            now: LocalDateTime,
            sourceId: Long,
            discountMethod: DiscountMethod,
            orderItemsDiscountMap: Map<DiscountContext.Item, Money>): List<DiscountLine> {
            return orderItemsDiscountMap.map { DiscountLine(
                orderItemId = it.key.orderItemId,
                type = discountMethod,
                sourceId = sourceId,
                amount = it.value,
                createdAt = now,
            ) }
        }
    }
}