package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.order.domain.event.*
import kr.hhplus.be.server.shared.domain.DomainEvent

/**
 * 주문과 결제 프로세스가 분리된 새로운 이벤트 체이닝
 * 
 * 주문 프로세스:
 * 1. OrderSheetCreated -> CouponService.use()
 * 2. CouponApplied -> ProductService.validateAndReduceStock()
 * 3. StockDeducted -> 주문서 준비 완료
 * 
 * 결제 프로세스:
 * 4. PaymentRequested -> ExternalPG.processPayment()
 * 5. PaymentCompleted -> Order.complete()
 * 6. OrderCompleted -> 외부 시스템 알림 (랭킹 등)
 */
sealed class OrderEvent {
    
    // === 주문 프로세스 이벤트 ===
    
    /**
     * 기존 호환성을 위한 주문 생성 이벤트
     */
    data class Created(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.created"
    }

    /**
     * 주문서 생성 완료 이벤트
     * -> 쿠폰 적용 프로세스 시작
     */
    data class OrderSheetCreated(
        override val payload: OrderSheetCreatedPayload): DomainEvent<OrderSheetCreatedPayload>() {
        override val eventType: String = "order.sheet.created"
    }

    /**
     * 쿠폰 적용 완료 이벤트
     * -> 재고 검증 및 차감 프로세스 시작
     */
    data class CouponApplied(
        override val payload: CouponAppliedPayload): DomainEvent<CouponAppliedPayload>() {
        override val eventType: String = "order.coupon.applied"
    }

    /**
     * 재고 차감 완료 이벤트
     * -> 주문서 준비 완료
     */
    data class StockDeducted(
        override val payload: StockDeductedPayload): DomainEvent<StockDeductedPayload>() {
        override val eventType: String = "order.stock.deducted"
    }

    /**
     * 주문서 준비 완료 이벤트
     * -> 클라이언트에서 결제 요청 가능 상태
     */
    data class Prepared(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.prepared"
    }
    
    // === 결제 프로세스 이벤트 ===
    
    /**
     * 결제 요청 이벤트
     * -> 외부 PG 결제 처리 시작
     */
    data class PaymentRequested(
        override val payload: PaymentRequestPayload): DomainEvent<PaymentRequestPayload>() {
        override val eventType: String = "order.payment.requested"
    }

    /**
     * 결제 완료 이벤트
     * -> 주문 완료 처리
     */
    data class PaymentCompleted(
        override val payload: PaymentCompletedPayload): DomainEvent<PaymentCompletedPayload>() {
        override val eventType: String = "order.payment.completed"
    }

    /**
     * 주문 완료 이벤트
     * -> 외부 시스템 알림 (랭킹, 알림 등)
     */
    data class Completed(
        override val payload: OrderCompletedPayload): DomainEvent<OrderCompletedPayload>() {
        override val eventType: String = "order.completed"
    }
    
    // === 실패 처리 이벤트 ===

    /**
     * 주문 실패 이벤트
     */
    data class Failed(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.failed"
    }

    /**
     * 쿠폰 적용 실패 이벤트
     */
    data class CouponApplyFailed(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.coupon.apply.failed"
    }

    /**
     * 재고 차감 실패 이벤트
     */
    data class StockDeductionFailed(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.stock.deduction.failed"
    }

    /**
     * 결제 실패 이벤트
     */
    data class PaymentFailed(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "order.payment.failed"
    }
}