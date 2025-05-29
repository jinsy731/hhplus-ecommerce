package kr.hhplus.be.server.point.domain

import kr.hhplus.be.server.shared.domain.DomainEvent
import java.time.LocalDateTime

data class UserPointEventPayload(
    val userId: Long,
    val orderId: Long,
    val amount: kr.hhplus.be.server.shared.domain.Money,
    val remainingBalance: kr.hhplus.be.server.shared.domain.Money,
    val timestamp: LocalDateTime,
    val failedReason: String? = null
)

class UserPointEvent {
    data class Deducted(
        override val payload: UserPointEventPayload): DomainEvent<UserPointEventPayload>() {
        override val eventType: String = "userPoint.deducted"
    }

    data class DeductionFailed(
        override val payload: UserPointEventPayload): DomainEvent<UserPointEventPayload>() {
        override val eventType: String = "userPoint.deduction-failed"
    }
}