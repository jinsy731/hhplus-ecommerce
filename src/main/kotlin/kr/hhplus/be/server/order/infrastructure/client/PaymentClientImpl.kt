package kr.hhplus.be.server.order.infrastructure.client

import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.infrastructure.client.mapper.OrderClientMapper
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.payment.application.mapper.PaymentMapper
import kr.hhplus.be.server.point.application.UserPointService
import org.springframework.stereotype.Component

/**
 * PaymentClient의 구현체
 * Order 도메인의 요청을 UserPoint와 Payment 도메인의 요청으로 변환하여 처리
 * - ExternalPgService 직접 사용을 제거하고 PaymentService에 위임
 * - DTO 매핑 로직을 PaymentMapper와 OrderClientMapper로 분리
 */
@Component
class PaymentClientImpl(
    private val userPointService: UserPointService,
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
    private val paymentMapper: PaymentMapper,
    private val orderClientMapper: OrderClientMapper
) : PaymentClient {
    
    override fun deductUserPoint(request: DeductUserPointRequest): Result<DeductUserPointResponse> {
        return runCatching {
            val userPointCommand = orderClientMapper.mapToUserPointUseCommand(request)
            
            // UserPoint 서비스 호출
            val result = userPointService.use(userPointCommand)
            
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: RuntimeException("포인트 차감 실패")
            }
            
            val userPoint = result.getOrThrow()
            
            // UserPoint 도메인의 응답을 Order 도메인의 응답으로 변환
            orderClientMapper.mapToDeductUserPointResponse(request, userPoint)
        }
    }
    
    override fun processPayment(request: ProcessPaymentRequest): Result<ProcessPaymentResponse> {
        return runCatching {
            // 1. 주문 조회 (OrderInfo로 조회하여 lazy loading 문제 해결)
            val orderInfo = orderService.getOrderInfoById(request.orderId)
            
            // 2. Payment 엔티티 생성 (결제 준비)
            val paymentPrepareCommand = paymentMapper.toPaymentPrepareCommand(orderInfo, request.timestamp)
            val paymentResult = paymentService.preparePayment(paymentPrepareCommand)
            
            if (paymentResult.isFailure) {
                throw RuntimeException("결제 준비 실패: ${paymentResult.exceptionOrNull()?.message}")
            }
            
            val payment = paymentResult.getOrThrow()
            
            // 3. PG를 통한 결제 처리 (PaymentService에 위임)
            val processWithPgCommand = paymentMapper.toProcessWithPgCommand(request, payment.id)
            val processResult = paymentService.processPaymentWithPg(processWithPgCommand)
            
            if (processResult.isFailure) {
                throw RuntimeException("결제 처리 실패: ${processResult.exceptionOrNull()?.message}")
            }
            
            // 4. 결과 변환 및 반환
            val result = processResult.getOrThrow()
            paymentMapper.toProcessPaymentResponse(result)
        }
    }
    
    override fun restoreUserPoint(request: RestoreUserPointRequest): Result<Unit> {
        return runCatching {
            val restoreCommand = orderClientMapper.mapToUserPointRestoreCommand(request)
            
            // UserPoint 서비스 호출
            userPointService.restore(restoreCommand)
        }
    }

    override fun failPayment(request: FailPaymentRequest): Result<FailPaymentResponse> {
        return runCatching {
            // 1. FailPaymentRequest를 PaymentCommand.Fail로 변환
            val failCommand = paymentMapper.toFailPaymentCommand(request)
            
            // 2. PaymentService의 failPayment 호출
            val failResult = paymentService.failPayment(failCommand)
            
            if (failResult.isFailure) {
                throw RuntimeException("결제 실패 처리 실패: ${failResult.exceptionOrNull()?.message}")
            }
            
            // 3. 결과 변환 및 반환
            val result = failResult.getOrThrow()
            paymentMapper.toFailPaymentResponse(result)
        }
    }
} 