package kr.hhplus.be.server.coupon.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
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

}