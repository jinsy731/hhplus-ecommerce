package kr.hhplus.be.server.coupon.infrastructure.event

import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.coupon.domain.port.CouponEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ApplicationCouponEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : CouponEventPublisher {
    
    override fun publishIssueRequested(event: CouponEvent.IssueRequested) {
        applicationEventPublisher.publishEvent(event)
    }
} 