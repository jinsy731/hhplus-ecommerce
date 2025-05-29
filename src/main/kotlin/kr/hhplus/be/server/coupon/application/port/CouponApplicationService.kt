package kr.hhplus.be.server.coupon.application.port

import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.coupon.application.dto.CouponResult
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import org.springframework.data.domain.Pageable

interface CouponApplicationService {
    fun issueCoupon(cmd: CouponCommand.Issue): CouponResult.Issue
    fun issueCouponAsync(cmd: CouponCommand.Issue): CouponResult.AsyncIssue
    fun use(cmd: CouponCommand.Use.Root): Result<CouponResult.Use>
    fun restoreCoupons(orderId: Long, orderEventPayload: OrderEventPayload)
    fun retrieveLists(userId: Long, pageable: Pageable): CouponResult.RetrieveList
    fun getUsedCouponIdsByOrderId(orderId: Long): List<Long>
} 