package kr.hhplus.be.server.order.infrastructure.client.mapper

import kr.hhplus.be.server.coupon.application.dto.CouponCommand
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.event.OrderEventPayload
import kr.hhplus.be.server.point.application.UserPointCommand
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.shared.domain.Money
import org.springframework.stereotype.Component

/**
 * Order 도메인의 클라이언트 구현체들에서 사용하는 공통 매핑 로직을 담당
 * 중복 코드를 제거하고 매핑 로직의 일관성을 보장
 */
@Component
class OrderClientMapper {
    
    // === Coupon 관련 매핑 ===
    
    /**
     * UseCouponRequest를 CouponCommand.Use.Root로 변환
     */
    fun mapToCouponUseCommand(request: UseCouponRequest): CouponCommand.Use.Root {
        val totalAmount = Money.of(
            request.orderItems.sumOf { it.price * it.quantity }
        )
        
        return CouponCommand.Use.Root(
            orderId = request.orderId,
            userId = request.userId,
            userCouponIds = request.userCouponIds,
            totalAmount = totalAmount,
            items = request.orderItems.map { couponOrderItem ->
                CouponCommand.Use.Item(
                    orderItemId = couponOrderItem.orderItemId,
                    productId = couponOrderItem.productId,
                    variantId = couponOrderItem.variantId,
                    quantity = couponOrderItem.quantity,
                    subTotal = Money.of(couponOrderItem.price * couponOrderItem.quantity)
                )
            },
            timestamp = request.timestamp,
        )
    }
    
    /**
     * 쿠폰 사용 결과를 UseCouponResponse로 변환
     */
    fun mapToUseCouponResponse(
        orderId: Long, 
        couponResult: kr.hhplus.be.server.coupon.application.dto.CouponResult.Use
    ): UseCouponResponse {
        return UseCouponResponse(
            orderId = orderId,
            appliedDiscounts = couponResult.discountInfo.map { discountInfo ->
                AppliedDiscount(
                    orderItemId = discountInfo.orderItemId,
                    sourceId = discountInfo.sourceId,
                    discountAmount = discountInfo.amount.amount.toLong(),
                    discountType = mapToDiscountType(discountInfo.sourceType)
                )
            }
        )
    }
    
    /**
     * RestoreCouponRequest를 OrderEventPayload로 변환
     */
    fun mapToOrderEventPayload(request: RestoreCouponRequest): OrderEventPayload {
        return OrderEventPayload(
            orderId = request.orderId,
            userId = 0L, // 복구 시에는 orderId로 찾으므로 불필요
            originalTotal = Money.ZERO,
            discountedAmount = Money.ZERO,
            orderItems = emptyList(),
            timestamp = request.timestamp,
            failedReason = "재고 차감 실패로 인한 쿠폰 복구"
        )
    }
    
    private fun mapToDiscountType(sourceType: String): DiscountType {
        return when (sourceType) {
            "COUPON" -> DiscountType.FIXED_AMOUNT
            "PERCENTAGE_COUPON" -> DiscountType.PERCENTAGE
            else -> DiscountType.FIXED_AMOUNT
        }
    }
    
    // === Product 관련 매핑 ===
    
    /**
     * ReduceStockRequest를 ProductCommand.ValidateAndReduceStock.Root로 변환
     */
    fun mapToValidateAndReduceStockCommand(request: ReduceStockRequest): ProductCommand.ValidateAndReduceStock.Root {
        return ProductCommand.ValidateAndReduceStock.Root(
            items = request.items.map { stockItem ->
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = stockItem.productId,
                    variantId = stockItem.variantId,
                    quantity = stockItem.quantity
                )
            },
            orderId = request.orderId
        )
    }
    
    /**
     * RestoreStockRequest를 ProductCommand.RestoreStock.Root로 변환
     */
    fun mapToRestoreStockCommand(request: RestoreStockRequest): ProductCommand.RestoreStock.Root {
        return ProductCommand.RestoreStock.Root(
            items = request.items.map { stockItem ->
                ProductCommand.RestoreStock.Item(
                    productId = stockItem.productId,
                    variantId = stockItem.variantId,
                    quantity = stockItem.quantity
                )
            },
            orderId = request.orderId
        )
    }
    
    /**
     * ReduceStockRequest를 ReduceStockResponse로 변환
     */
    fun mapToReduceStockResponse(request: ReduceStockRequest): ReduceStockResponse {
        return ReduceStockResponse(
            orderId = request.orderId,
            processedItems = request.items.map { stockItem ->
                ProcessedStockItem(
                    productId = stockItem.productId,
                    requestedQuantity = stockItem.quantity,
                    processedQuantity = stockItem.quantity
                )
            }
        )
    }
    
    // === UserPoint 관련 매핑 ===
    
    /**
     * DeductUserPointRequest를 UserPointCommand.Use로 변환
     */
    fun mapToUserPointUseCommand(request: DeductUserPointRequest): UserPointCommand.Use {
        return UserPointCommand.Use(
            userId = request.userId,
            amount = request.amount,
            orderId = request.orderId,
            now = request.timestamp
        )
    }
    
    /**
     * RestoreUserPointRequest를 UserPointCommand.Restore로 변환
     */
    fun mapToUserPointRestoreCommand(request: RestoreUserPointRequest): UserPointCommand.Restore {
        return UserPointCommand.Restore(
            userId = request.userId,
            amount = request.amount,
            orderId = request.orderId,
            now = request.timestamp
        )
    }
    
    /**
     * UserPoint 사용 결과를 DeductUserPointResponse로 변환
     */
    fun mapToDeductUserPointResponse(
        request: DeductUserPointRequest,
        userPoint: UserPoint
    ): DeductUserPointResponse {
        return DeductUserPointResponse(
            userId = userPoint.userId,
            deductedAmount = request.amount,
            remainingBalance = userPoint.balance
        )
    }
} 