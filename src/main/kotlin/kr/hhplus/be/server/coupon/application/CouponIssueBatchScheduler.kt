package kr.hhplus.be.server.coupon.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CouponIssueBatchScheduler(
    private val couponIssueBatchService: CouponIssueBatchService
) {
    
    /**
     * 2초 간격으로 쿠폰 발급 요청 처리
     */
    @Scheduled(fixedDelay = 100)
    fun processIssueRequest() {
        couponIssueBatchService.processIssueRequest()
    }
    
    /**
     * 2초 간격으로 실패한 쿠폰 발급 요청 처리
     */
    @Scheduled(fixedDelay = 100)
    fun processFailedIssueRequest() {
        couponIssueBatchService.processFailedIssueRequest()
    }
} 