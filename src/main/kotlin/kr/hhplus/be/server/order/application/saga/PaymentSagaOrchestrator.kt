package kr.hhplus.be.server.order.application.saga

import kr.hhplus.be.server.order.application.*
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload
import kr.hhplus.be.server.order.domain.model.Order
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 결제 Saga 단계를 나타내는 enum
 */
enum class PaymentStep {
    DEDUCT_POINT,
    PROCESS_PAYMENT,
    COMPLETE_ORDER
}

/**
 * Payment Saga 실행 컨텍스트 - 보상 로직에서 필요한 정보를 담고 있음
 */
private data class PaymentSagaContext(
    val cmd: OrderCommand.ProcessPayment.Root,
    val orderInfo: OrderInfo? = null,
    val pointDeductResult: DeductUserPointResponse? = null,
    val paymentResult: ProcessPaymentResponse? = null,
    val completedSteps: MutableList<PaymentStep> = mutableListOf()
)

/**
 * 결제 처리 Saga Orchestrator
 * 유저 포인트 차감 -> 결제 수행 -> 주문 완료 처리의 복잡한 플로우를 관리
 */
@Service
class PaymentSagaOrchestrator(
    private val orderService: OrderService,
    private val paymentClient: PaymentClient,
    private val orderSagaOrchestrator: OrderSagaOrchestrator,
    private val couponClient: CouponClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 결제 처리 Saga 실행
     * 1. 유저 포인트 차감
     * 2. 결제 수행 (PG 연동)
     * 3. 주문 완료 처리
     * 
     * 실패 시 보상 트랜잭션 실행 후 예외 던짐
     */
//    @Transactional
    fun executePaymentSaga(cmd: OrderCommand.ProcessPayment.Root): Order {
        logger.info("Starting payment saga for orderId: {}", cmd.orderId)
        
        var currentContext = PaymentSagaContext(cmd)
        
        try {
            // 1. 주문 조회 (OrderInfo로 조회하여 lazy loading 문제 해결)
            val orderInfo = getOrderInfo(cmd.orderId)
            currentContext = currentContext.copy(orderInfo = orderInfo)
            
            // 2. 유저 포인트 차감
            val pointDeductResult = deductUserPoint(orderInfo, cmd.timestamp)
            currentContext = currentContext.copy(
                pointDeductResult = pointDeductResult,
                completedSteps = currentContext.completedSteps.apply { add(PaymentStep.DEDUCT_POINT) }
            )
            
            // 3. 결제 수행
            val paymentResult = processPayment(orderInfo, cmd)
            currentContext = currentContext.copy(
                paymentResult = paymentResult,
                completedSteps = currentContext.completedSteps.apply { add(PaymentStep.PROCESS_PAYMENT) }
            )
            
            // 4. 주문 완료 처리
            completeOrder(cmd.orderId, paymentResult, cmd.timestamp)
            currentContext = currentContext.copy(
                completedSteps = currentContext.completedSteps.apply { add(PaymentStep.COMPLETE_ORDER) }
            )
            
            logger.info("결제 처리 완료: {}", orderInfo.id)
            return orderService.getOrderById(cmd.orderId)
            
        } catch (exception: Exception) {
            logger.error("Saga 실패: {}", exception.message, exception)
            compensate(currentContext, exception)
            throw exception
        }
    }
    
    private fun getOrderInfo(orderId: Long): OrderInfo {
        val orderInfo = orderService.getOrderInfoById(orderId)
        logger.info("Order retrieved for payment: {}", orderInfo.id)
        return orderInfo
    }
    
    /**
     * 유저 포인트 차감
     */
    private fun deductUserPoint(orderInfo: OrderInfo, timestamp: LocalDateTime): DeductUserPointResponse {
        val request = orderInfo.toDeductUserPointRequest(timestamp)
        
        return paymentClient.deductUserPoint(request)
            .onSuccess { result ->
                logger.info("User point deducted for order: {}, remaining balance: {}", 
                    orderInfo.id, result.remainingBalance)
            }
            .onFailure { 
                logger.error("Failed to deduct user point for order: {}, error: {}", orderInfo.id, it.message)
                throw PaymentSagaException("유저 포인트 차감 실패", it)
            }
            .getOrThrow()
    }
    
    /**
     * 결제 수행
     */
    private fun processPayment(orderInfo: OrderInfo, cmd: OrderCommand.ProcessPayment.Root): ProcessPaymentResponse {
        val request = orderInfo.toProcessPaymentRequest(
            pgPaymentId = cmd.pgPaymentId,
            paymentMethod = cmd.paymentMethod,
            timestamp = cmd.timestamp
        )
        
        return paymentClient.processPayment(request)
            .onSuccess { result ->
                logger.info("Payment processed for order: {}, paymentId: {}", 
                    orderInfo.id, result.paymentId)
            }
            .onFailure {
                logger.error("Failed to process payment for order: {}, error: {}", orderInfo.id, it.message)
                throw PaymentSagaException("결제 처리 실패", it)
            }
            .getOrThrow()
    }
    
    /**
     * 주문 완료 처리
     */
    private fun completeOrder(orderId: Long, paymentResult: ProcessPaymentResponse, timestamp: LocalDateTime) {
        // OrderService의 completeOrder를 사용하여 중복 이벤트 방지
        val paymentCompletedPayload = PaymentCompletedPayload(
            orderId = orderId,
            userId = orderService.getOrderInfoById(orderId).userId,
            paymentId = paymentResult.paymentId,
            pgPaymentId = paymentResult.pgPaymentId,
            amount = paymentResult.paidAmount,
            timestamp = timestamp
        )
        
        orderService.completeOrder(orderId, paymentCompletedPayload)
            .onSuccess { 
                logger.info("Order completed for order: {}", orderId) 
            }
            .onFailure { 
                throw PaymentSagaException("주문 완료 처리 실패", it)
            }
    }
    
    /**
     * 보상 로직 실행
     */
    private fun compensate(context: PaymentSagaContext, ex: Throwable) {
        logger.info("보상 로직 시작 for orderId: {}, 실패 이유: {}", context.orderInfo?.id ?: "unknown", ex.message)
        
        context.completedSteps.reversed().forEach { step ->
            when (step) {
                PaymentStep.COMPLETE_ORDER -> {
                    logger.info("주문 완료 보상 실행")
                    compensateOrderCompletion(context)
                }
                PaymentStep.PROCESS_PAYMENT -> {
                    logger.info("결제 보상 실행")
                    compensatePayment(context)
                }
                PaymentStep.DEDUCT_POINT -> {
                    logger.info("포인트 보상 실행")
                    compensateUserPoint(context)
                }
            }
        }
        
        // OrderSaga 보상 실행 - 재고 복구, 쿠폰 복구
        compensateOrderSaga(context)

        failOrder(context)
    }
    
    /**
     * 주문 완료 보상 (보상 트랜잭션)
     * 주문 상태를 CREATED로 되돌림
     */
    private fun compensateOrderCompletion(context: PaymentSagaContext) {
        val orderInfo = context.orderInfo
        logger.info("Starting order completion compensation for order: {}", orderInfo?.id)
        
        if (orderInfo != null) {
            runCatching {
                logger.info("Successfully compensated order completion for order: {}", orderInfo.id)
            }.onFailure { exception ->
                logger.error("Failed to compensate order completion for order: {}, error: {}", orderInfo.id, exception.message)
            }
        } else {
            logger.warn("Cannot compensate order completion - order is null")
        }
    }
    
    /**
     * 결제 보상 (보상 트랜잭션)
     */
    private fun compensatePayment(context: PaymentSagaContext) {
        val orderInfo = context.orderInfo
        val paymentResult = context.paymentResult
        logger.info("Starting payment compensation for order: {}", orderInfo?.id)
        
        if (orderInfo != null && paymentResult != null) {
            // 결제 실패 처리
            val failPaymentRequest = createFailPaymentRequest(
                orderId = orderInfo.id,
                paymentId = paymentResult.paymentId,
                pgPaymentId = paymentResult.pgPaymentId,
                failedReason = "결제 처리 중 오류 발생으로 인한 보상 처리",
                timestamp = context.cmd.timestamp
            )
            
            paymentClient.failPayment(failPaymentRequest)
                .onSuccess { 
                    logger.info("Successfully failed payment for order: {}", orderInfo.id) 
                }
                .onFailure { 
                    logger.error("Failed to fail payment for order: {}, error: {}", orderInfo.id, it.message) 
                }
                
            logger.info("Successfully compensated payment for order: {}", orderInfo.id)
        } else {
            logger.warn("Cannot compensate payment - order or paymentResult is null")
        }
    }
    
    /**
     * 유저 포인트 복구 (보상 트랜잭션)
     */
    private fun compensateUserPoint(context: PaymentSagaContext) {
        val orderInfo = context.orderInfo
        logger.info("Starting user point compensation for order: {}", orderInfo?.id)
        
        if (orderInfo != null) {
            val request = orderInfo.toRestoreUserPointRequest(context.cmd.timestamp)
            paymentClient.restoreUserPoint(request)
                .onSuccess { logger.info("Successfully restored user point for order: {}", orderInfo.id) }
                .onFailure { logger.error("Failed to restore user point for order: {}, error: {}", orderInfo.id, it.message) }
        } else {
            logger.warn("Cannot compensate user point - order is null")
        }
    }
    
    /**
     * OrderSaga 보상 실행
     * 재고 복구, 쿠폰 복구를 수행
     */
    private fun compensateOrderSaga(context: PaymentSagaContext) {
        val orderInfo = context.orderInfo
        if (orderInfo != null) {
            logger.info("Starting OrderSaga compensation for order: {}", orderInfo.id)
            
            // 주문에 사용된 쿠폰 ID 목록 조회
            val usedCouponIds = runCatching {
                couponClient.getUsedCouponIdsByOrderId(orderInfo.id)
            }.getOrElse { 
                logger.warn("Failed to get used coupon IDs for order: {}, proceeding without coupon compensation", orderInfo.id)
                emptyList()
            }
            
            // OrderSagaOrchestrator의 보상 메서드 호출
            try {
                orderSagaOrchestrator.compensateOrderSaga(orderInfo.id, usedCouponIds)
                logger.info("Successfully compensated OrderSaga for order: {}", orderInfo.id)
            } catch (exception: Exception) {
                logger.error("Failed to compensate OrderSaga for order: {}, error: {}", orderInfo.id, exception.message)
            }
        } else {
            logger.warn("Cannot compensate OrderSaga - order is null")
        }
    }
    
    /**
     * 주문 실패 처리
     */
    private fun failOrder(context: PaymentSagaContext) {
        val orderInfo = context.orderInfo
        if (orderInfo != null) {
            logger.info("Starting order failure for order: {}", orderInfo.id)
            
            runCatching {
                orderService.fail(orderInfo.id)
                logger.info("Successfully failed order: {}", orderInfo.id)
            }.onFailure { exception ->
                logger.error("Failed to fail order: {}, error: {}", orderInfo.id, exception.message)
            }
        } else {
            logger.warn("Cannot fail order - order is null")
        }
    }
}

/**
 * Payment Saga 실행 중 발생하는 예외
 */
class PaymentSagaException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) 