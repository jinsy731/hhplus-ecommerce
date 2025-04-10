package kr.hhplus.be.server.order.domain.model

import kr.hhplus.be.server.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "discount_lines")
class DiscountLine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: DiscountType,
    
    @Column
    val sourceId: Long?,
    
    @Column(nullable = false)
    val amount: BigDecimal,
    
    @Column(nullable = false)
    val description: String,

    @ManyToOne(cascade = [CascadeType.ALL], optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null
) : BaseTimeEntity() {


    companion object {
        /**
         * 쿠폰에서 할인 내역 생성
         */
        fun fromCoupon(couponId: Long, amount: BigDecimal, description: String): DiscountLine {
            return DiscountLine(
                type = DiscountType.COUPON,
                sourceId = couponId,
                amount = amount,
                description = description
            )
        }
        
        /**
         * 포인트 사용 할인 내역 생성
         */
        fun fromPoint(amount: BigDecimal): DiscountLine {
            return DiscountLine(
                type = DiscountType.POINT,
                sourceId = null,
                amount = amount,
                description = "포인트 사용"
            )
        }
    }
}
