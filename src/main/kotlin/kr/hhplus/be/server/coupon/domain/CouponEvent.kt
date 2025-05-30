package kr.hhplus.be.server.coupon.domain

import kr.hhplus.be.server.order.domain.event.CouponAppliedPayload
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.shared.domain.DomainEvent
import java.time.LocalDateTime

class CouponEvent {
    data class IssueRequested(
        override val payload: CouponIssueRequestedPayload
    ): DomainEvent<CouponIssueRequestedPayload>() {
        override val eventType: String = "coupon_issue-requested"
    }

    data class Used(override val payload: CouponAppliedPayload): DomainEvent<CouponAppliedPayload>() {
        override val eventType: String = "coupon.used"
    }

    data class UseFailed(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "coupon.use-failed"
    }

    data class UseRestored(
        override val payload: OrderEventPayload): DomainEvent<OrderEventPayload>() {
        override val eventType: String = "coupon.use-restored"
    }
}

data class CouponIssueRequestedPayload(
    val userId: Long,
    val couponId: Long,
    val issuedAt: LocalDateTime
)