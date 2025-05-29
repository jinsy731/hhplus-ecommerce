package kr.hhplus.be.server.order.infrastructure.client

import kr.hhplus.be.server.coupon.application.port.CouponApplicationService
import kr.hhplus.be.server.order.domain.client.CouponClient
import kr.hhplus.be.server.order.domain.client.RestoreCouponRequest
import kr.hhplus.be.server.order.domain.client.UseCouponRequest
import kr.hhplus.be.server.order.domain.client.UseCouponResponse
import kr.hhplus.be.server.order.infrastructure.client.mapper.OrderClientMapper
import org.springframework.stereotype.Component

/**
 * CouponClient의 구현체
 * Order 도메인의 요청을 Coupon 도메인의 요청으로 변환하여 처리
 */
@Component
class CouponClientImpl(
    private val couponService: CouponApplicationService,
    private val orderClientMapper: OrderClientMapper
) : CouponClient {
    
    override fun useCoupons(request: UseCouponRequest): Result<UseCouponResponse> {
        return runCatching {
            val couponCommand = orderClientMapper.mapToCouponUseCommand(request)
            
            // Coupon 서비스 호출
            val result = couponService.use(couponCommand)
            
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: RuntimeException("쿠폰 사용 실패")
            }
            
            val couponResult = result.getOrThrow()
            
            // Coupon 도메인의 응답을 Order 도메인의 응답으로 변환
            orderClientMapper.mapToUseCouponResponse(request.orderId, couponResult)
        }
    }
    
    override fun restoreCoupons(request: RestoreCouponRequest): Result<Unit> {
        return runCatching {
            val orderEventPayload = orderClientMapper.mapToOrderEventPayload(request)
            
            couponService.restoreCoupons(
                orderId = request.orderId,
                orderEventPayload = orderEventPayload
            )
        }
    }
    
    override fun getUsedCouponIdsByOrderId(orderId: Long): List<Long> {
        return couponService.getUsedCouponIdsByOrderId(orderId)
    }
} 