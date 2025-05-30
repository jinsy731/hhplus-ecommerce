package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.CouponEvent

interface CouponEventPublisher {
    fun publishIssueRequested(event: CouponEvent.IssueRequested)
} 