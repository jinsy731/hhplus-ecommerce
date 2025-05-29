package kr.hhplus.be.server.order.domain.event

import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.shared.domain.Money
import java.time.LocalDateTime

/**
 * 이벤트 페이로드에 사용할 DTO들
 * Order 도메인 모델의 직접 사용을 피하여 순환 참조 문제를 해결합니다.
 */

/**
 * 주문 이벤트 기본 페이로드
 */
data class OrderEventPayload(
    val orderId: Long,
    val userId: Long,
    val originalTotal: Money,
    val discountedAmount: Money,
    val orderItems: List<OrderItemEventPayload>,
    val timestamp: LocalDateTime,
    val userCouponIds: List<Long> = emptyList(),
    val discountInfos: List<DiscountInfo> = emptyList(),
    val paymentId: Long? = null,
    val pgPaymentId: String? = null,
    val failedReason: String? = null
)

/**
 * 주문 항목 이벤트 페이로드
 */
data class OrderItemEventPayload(
    val id: Long,
    val productId: Long,
    val variantId: Long,
    val quantity: Int,
    val unitPrice: Money,
    val discountAmount: Money = Money.ZERO
)

/**
 * 주문서 생성 이벤트 페이로드
 */
data class OrderSheetCreatedPayload(
    val orderId: Long,
    val userId: Long,
    val originalTotal: Money,
    val orderItems: List<OrderItemEventPayload>,
    val userCouponIds: List<Long>,
    val timestamp: LocalDateTime
)

/**
 * 결제 처리 요청 이벤트 페이로드
 */
data class PaymentRequestPayload(
    val orderId: Long,
    val userId: Long,
    val amount: Money,
    val pgPaymentId: String,
    val paymentMethod: String,
    val timestamp: LocalDateTime
)

/**
 * 결제 완료 이벤트 페이로드
 */
data class PaymentCompletedPayload(
    val orderId: Long,
    val userId: Long,
    val paymentId: Long,
    val pgPaymentId: String,
    val amount: Money,
    val timestamp: LocalDateTime
)

/**
 * 쿠폰 적용 완료 이벤트 페이로드
 */
data class CouponAppliedPayload(
    val orderId: Long,
    val userId: Long,
    val userCouponIds: List<Long>,
    val discountInfos: List<DiscountInfo>,
    val timestamp: LocalDateTime
)

/**
 * 재고 차감 완료 이벤트 페이로드
 */
data class StockDeductedPayload(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItemEventPayload>,
    val timestamp: LocalDateTime
)

/**
 * 주문 완료 이벤트 페이로드
 */
data class OrderCompletedPayload(
    val orderId: Long,
    val userId: Long,
    val originalTotal: Money,
    val finalTotal: Money,
    val orderItems: List<OrderItemEventPayload>,
    val paymentId: Long,
    val pgPaymentId: String,
    val timestamp: LocalDateTime
) 