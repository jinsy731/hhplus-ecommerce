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
    val orderId: Long,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: DiscountType,
    
    @Column
    val sourceId: Long?,
    
    @Column(nullable = false)
    val amount: BigDecimal,
    
    @Column(nullable = false)
    val description: String
) : BaseTimeEntity() {
    
    companion object {
        /**
         * 쿠폰에서 할인 내역 생성
         */
        fun fromCoupon(orderId: Long, couponId: Long, amount: BigDecimal, description: String): DiscountLine {
            return DiscountLine(
                orderId = orderId,
                type = DiscountType.COUPON,
                sourceId = couponId,
                amount = amount,
                description = description
            )
        }
        
        /**
         * 포인트 사용 할인 내역 생성
         */
        fun fromPoint(orderId: Long, amount: BigDecimal): DiscountLine {
            return DiscountLine(
                orderId = orderId,
                type = DiscountType.POINT,
                sourceId = null,
                amount = amount,
                description = "포인트 사용"
            )
        }
    }
}
