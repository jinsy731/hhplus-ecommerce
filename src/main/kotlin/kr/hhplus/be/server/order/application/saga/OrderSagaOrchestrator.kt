package kr.hhplus.be.server.order.application.saga

import kr.hhplus.be.server.order.application.*
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.model.Order
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 주문 Saga 단계를 나타내는 enum
 */
enum class OrderStep {
    CREATE_ORDER,
    APPLY_COUPON,
    REDUCE_STOCK
}

/**
 * Saga 실행 컨텍스트 - 보상 로직에서 필요한 정보를 담고 있음
 */
private data class SagaContext(
    val cmd: OrderCommand.CreateOrderSheet.Root,
    val orderInfo: OrderInfo? = null,
    val completedSteps: MutableList<OrderStep> = mutableListOf()
)

/**
 * 주문서 생성 Saga Orchestrator
 * 주문서 생성 -> 쿠폰 적용 -> 재고 차감의 복잡한 플로우를 관리
 */
@Service
class OrderSagaOrchestrator(
    private val orderService: OrderService,
    private val couponClient: CouponClient,
    private val productClient: ProductClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 주문서 생성 Saga 실행
     * 1. 주문서 생성
     * 2. 쿠폰 적용 (있는 경우)
     * 3. 재고 차감
     * 
     * 실패 시 보상 트랜잭션 실행 후 예외 던짐
     */
//    @Transactional
    fun executeOrderSheetCreationSaga(cmd: OrderCommand.CreateOrderSheet.Root): Order {
        logger.info("Starting order sheet creation saga for userId: {}", cmd.userId)
        
        var currentContext = SagaContext(cmd)
        
        try {
            // 1. 주문서 생성
            val order = createOrderSheet(cmd)
            val orderInfo = orderService.getOrderInfoById(order.id!!)
            currentContext = currentContext.copy(
                orderInfo = orderInfo,
                completedSteps = currentContext.completedSteps.apply { add(OrderStep.CREATE_ORDER) }
            )
            
            // 2. 쿠폰 적용 (필요한 경우)
            applyCouponsIfNeeded(orderInfo, cmd)
            currentContext = currentContext.copy(
                completedSteps = currentContext.completedSteps.apply { add(OrderStep.APPLY_COUPON) }
            )
            
            // 3. 재고 차감 (쿠폰 적용 후 최신 정보 재조회)
            val updatedOrderInfo = orderService.getOrderInfoById(orderInfo.id)
            currentContext = currentContext.copy(orderInfo = updatedOrderInfo)
            reduceStock(updatedOrderInfo)
            currentContext = currentContext.copy(
                completedSteps = currentContext.completedSteps.apply { add(OrderStep.REDUCE_STOCK) }
            )
            
            logger.info("주문 처리 완료: {}", orderInfo.id)
            return order
            
        } catch (exception: Exception) {
            logger.error("Saga 실패: {}", exception.message, exception)
            compensate(currentContext, cmd, exception)
            throw exception
        }
    }
    
    /**
     * 외부에서 호출 가능한 보상 메서드
     * PaymentSaga 실패 시 OrderSaga의 모든 작업을 롤백
     */
    @Transactional
    fun compensateOrderSaga(orderId: Long, userCouponIds: List<Long>) {
        try {
            logger.info("Starting order saga compensation for orderId: {}", orderId)
            
            // 보상에서는 OrderInfo를 사용하여 lazy loading 문제 해결
            val orderInfo = orderService.getOrderInfoById(orderId)
            
            // 재고 복구
            compensateStock(orderInfo)
            
            // 쿠폰 복구 (쿠폰이 사용된 경우에만)
            if (userCouponIds.isNotEmpty()) {
                compensateCoupons(orderInfo, userCouponIds)
            }
            
            // 주문 상태 변경은 PaymentSagaOrchestrator에서 처리하므로 제외
            
            logger.info("Order saga compensation completed for orderId: {}", orderId)
        } catch (exception: Exception) {
            logger.error("Failed to compensate order saga for orderId: {}, error: {}", orderId, exception.message)
            throw exception
        }
    }
    
    private fun createOrderSheet(cmd: OrderCommand.CreateOrderSheet.Root): Order {
        val order = orderService.createOrderSheet(cmd)
        logger.info("Order sheet created with id: {}", order.id)
        return order
    }
    
    /**
     * 쿠폰 적용 (필요한 경우)
     * 비즈니스 로직: 쿠폰이 있으면 적용, 없으면 스킵
     */
    private fun applyCouponsIfNeeded(orderInfo: OrderInfo, cmd: OrderCommand.CreateOrderSheet.Root) {
        if (cmd.userCouponIds.isEmpty()) return

        val couponRequest = UseCouponRequest(
            orderId = orderInfo.id,
            userId = orderInfo.userId,
            userCouponIds = cmd.userCouponIds,
            orderItems = orderInfo.toCouponOrderItems(),
            timestamp = cmd.timestamp
        )
        
        val couponResult = couponClient.useCoupons(couponRequest)
            .getOrElse {
                logger.error("Failed to apply coupons for order: {}, error: {}", orderInfo.id, it.message)
                throw OrderSagaException("쿠폰 적용 실패", it)
            }
        
        // 할인 적용
        val discountInfos = couponResult.appliedDiscounts.map { discount ->
            kr.hhplus.be.server.coupon.application.dto.DiscountInfo(
                orderItemId = discount.orderItemId,
                amount = kr.hhplus.be.server.shared.domain.Money.of(discount.discountAmount),
                sourceId = discount.sourceId,
                sourceType = discount.discountType.name
            )
        }
        
        orderService.applyDiscountToOrder(orderInfo.id, discountInfos)
        logger.info("Coupons applied for order: {}", orderInfo.id)
        return
    }
    
    /**
     * 재고 차감
     */
    private fun reduceStock(orderInfo: OrderInfo): ReduceStockResponse {
        val stockRequest = ReduceStockRequest(
            orderId = orderInfo.id,
            items = orderInfo.toStockItems()
        )
        
        return productClient.validateAndReduceStock(stockRequest)
            .onSuccess { logger.info("Stock reduced for order: {}", orderInfo.id) }
            .onFailure { 
                logger.error("Failed to reduce stock for order: {}, error: {}", orderInfo.id, it.message)
                throw OrderSagaException("재고 차감 실패", it)
            }
            .getOrThrow()
    }
    
    /**
     * 보상 로직 실행
     */
    private fun compensate(context: SagaContext, cmd: OrderCommand.CreateOrderSheet.Root, ex: Throwable) {
        logger.info("보상 로직 시작 for orderId: {}, 실패 이유: {}", context.orderInfo?.id ?: "unknown", ex.message)
        
        context.completedSteps.reversed().forEach { step ->
            when (step) {
                OrderStep.REDUCE_STOCK -> {
                    logger.info("재고 보상 실행")
                    context.orderInfo?.let { compensateStock(it) }
                }
                OrderStep.APPLY_COUPON -> {
                    logger.info("쿠폰 보상 실행")
                    context.orderInfo?.let { compensateCoupons(it, cmd.userCouponIds) }
                }
                OrderStep.CREATE_ORDER -> {
                    logger.info("주문 보상 실행")
                    compensateOrderCreation(context)
                }
            }
        }
    }
    
    /**
     * 주문 생성 보상 (보상 트랜잭션)
     * 주문 상태를 실패로 변경
     */
    private fun compensateOrderCreation(context: SagaContext) {
        val orderId = context.orderInfo?.id
        logger.info("Starting order creation compensation for order: {}", orderId)
        
        runCatching {
            if (orderId != null) {
                orderService.fail(orderId)
                logger.info("Successfully marked order as failed: {}", orderId)
            } else {
                logger.warn("Cannot compensate order creation - orderId is null")
            }
        }.onFailure { exception ->
            logger.error("Failed to compensate order creation for order: {}, error: {}", orderId, exception.message)
        }
    }
    
    /**
     * 재고 복구 (보상 트랜잭션)
     */
    private fun compensateStock(orderInfo: OrderInfo) {
        logger.info("Starting stock compensation for order: {}", orderInfo.id)
        
        val restoreRequest = RestoreStockRequest(
            orderId = orderInfo.id,
            items = orderInfo.toStockItems()
        )
        
        productClient.restoreStock(restoreRequest)
            .onSuccess { logger.info("Successfully restored stock for order: {}", orderInfo.id) }
            .onFailure { logger.error("Failed to restore stock for order: {}, error: {}", orderInfo.id, it.message) }
    }
    
    /**
     * 쿠폰 복구 (보상 트랜잭션)
     */
    private fun compensateCoupons(orderInfo: OrderInfo, userCouponIds: List<Long>) {
        logger.info("Starting coupon compensation for order: {}", orderInfo.id)

        if (userCouponIds.isEmpty()) {
            logger.warn("Cannot compensate coupons - userCouponIds is empty")
            return
        }

        val restoreRequest = RestoreCouponRequest(
            orderId = orderInfo.id,
            userCouponIds = userCouponIds,
            timestamp = LocalDateTime.now()
        )

        couponClient.restoreCoupons(restoreRequest)
            .onSuccess { logger.info("Successfully restored coupons for order: {}", orderInfo.id) }
            .onFailure { logger.error("Failed to restore coupons for order: {}, error: {}", orderInfo.id, it.message) }
    }
}

/**
 * Saga 실행 중 발생하는 예외
 */
class OrderSagaException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) 