package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.application.saga.OrderSagaOrchestrator
import kr.hhplus.be.server.order.application.saga.PaymentSagaOrchestrator
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.product.application.port.ProductApplicationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val orderSagaOrchestrator: OrderSagaOrchestrator,
    private val paymentSagaOrchestrator: PaymentSagaOrchestrator,
    private val productService: ProductApplicationService,
    ) {

    /**
     * 주문서 생성
     * - 상품 유효성 검증
     * - Saga Orchestrator를 통한 복잡한 플로우 처리
     * - 주문서 생성 + 쿠폰 적용 + 재고 차감
     */
    @Transactional
    fun createOrderSheet(cri: OrderCriteria.CreateOrderSheet.Root): Order {
        val products = productService.findAllById(cri.items.map { it.productId })
        val command = cri.toCreateOrderSheetCommand(products)
        
        return try {
            orderSagaOrchestrator.executeOrderSheetCreationSaga(command)
        } catch (exception: Exception) {
            throw RuntimeException("주문서 생성 실패: ${exception.message}", exception)
        }
    }

    /**
     * 결제 처리
     * - PaymentSagaOrchestrator를 통한 복잡한 결제 플로우 처리
     * - 유저 포인트 차감 -> 결제 수행 -> 주문 완료
     */
    @Transactional
    fun processPayment(cri: OrderCriteria.ProcessPayment.Root): Order {
        return try {
            paymentSagaOrchestrator.executePaymentSaga(cri.toProcessPaymentCommand())
        } catch (exception: Exception) {
            throw RuntimeException("결제 처리 실패: ${exception.message}", exception)
        }
    }

    /**
     * 기존 주문 생성 (호환성 유지)
     * - 기존 이벤트 기반 처리 방식
     */
    @Transactional
    fun placeOrder(cri: OrderCriteria.PlaceOrder.Root): Order {
        val products = productService.findAllById(cri.items.map { it.productId })
        return orderService.createOrder(cri.toCreateOrderCommand(products))
    }
}