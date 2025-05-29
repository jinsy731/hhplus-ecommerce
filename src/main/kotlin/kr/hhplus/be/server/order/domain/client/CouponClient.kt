package kr.hhplus.be.server.order.domain.client

import kr.hhplus.be.server.order.domain.model.OrderItem
import java.time.LocalDateTime

/**
 * Order 도메인에서 Coupon 도메인을 호출하기 위한 클라이언트 인터페이스
 */
interface CouponClient {
    
    /**
     * 쿠폰 사용
     */
    fun useCoupons(request: UseCouponRequest): Result<UseCouponResponse>
    
    /**
     * 쿠폰 복구 (주문 실패 시)
     */
    fun restoreCoupons(request: RestoreCouponRequest): Result<Unit>
    
    /**
     * 주문에 사용된 쿠폰 ID 목록 조회
     */
    fun getUsedCouponIdsByOrderId(orderId: Long): List<Long>
}

/**
 * 쿠폰 사용 요청
 */
data class UseCouponRequest(
    val orderId: Long,
    val userId: Long,
    val userCouponIds: List<Long>,
    val orderItems: List<CouponOrderItem>,
    val timestamp: LocalDateTime
)

/**
 * 쿠폰 적용을 위한 주문 아이템
 */
data class CouponOrderItem(
    val orderItemId: Long,
    val productId: Long,
    val variantId: Long,
    val quantity: Int,
    val price: Long
)

/**
 * 쿠폰 사용 응답
 */
data class UseCouponResponse(
    val orderId: Long,
    val appliedDiscounts: List<AppliedDiscount>
)

/**
 * 적용된 할인 정보
 */
data class AppliedDiscount(
    val orderItemId: Long,
    val sourceId: Long,
    val discountAmount: Long,
    val discountType: DiscountType
)

/**
 * 할인 타입
 */
enum class DiscountType {
    FIXED_AMOUNT,
    PERCENTAGE
}

/**
 * 쿠폰 복구 요청
 */
data class RestoreCouponRequest(
    val orderId: Long,
    val userCouponIds: List<Long>,
    val timestamp: LocalDateTime
)

/**
 * Order 도메인 모델을 클라이언트 요청으로 변환하는 확장 함수들
 */
fun List<OrderItem>.toCouponOrderItems(): List<CouponOrderItem> {
    return this.map { orderItem ->
        CouponOrderItem(
            orderItemId = orderItem.id ?: throw IllegalStateException("OrderItem id must not be null after save"),
            productId = orderItem.productId,
            variantId = orderItem.variantId,
            quantity = orderItem.quantity,
            price = orderItem.unitPrice.amount.toLong()
        )
    }
} 