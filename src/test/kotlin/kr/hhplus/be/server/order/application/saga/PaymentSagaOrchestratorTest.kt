package kr.hhplus.be.server.order.application.saga

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowAny
import io.mockk.*
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderInfo
import kr.hhplus.be.server.order.application.OrderItemInfo
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.shared.TestEntityUtils
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PaymentSagaOrchestratorTest {

    private lateinit var paymentSagaOrchestrator: PaymentSagaOrchestrator
    private lateinit var orderService: OrderService
    private lateinit var paymentClient: PaymentClient
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var orderSagaOrchestrator: OrderSagaOrchestrator
    private lateinit var couponClient: CouponClient
    
    private lateinit var testOrder: Order
    private lateinit var testOrderInfo: OrderInfo
    private lateinit var paymentCommand: OrderCommand.ProcessPayment.Root
    
    @BeforeEach
    fun setUp() {
        orderService = mockk()
        paymentClient = mockk()
        eventPublisher = mockk()
        orderSagaOrchestrator = mockk()
        couponClient = mockk()
        
        paymentSagaOrchestrator = PaymentSagaOrchestrator(
            orderService = orderService,
            paymentClient = paymentClient,
            orderSagaOrchestrator = orderSagaOrchestrator,
            couponClient = couponClient,
        )
        
        // 테스트용 주문 생성
        testOrder = OrderTestFixture
            .order()
            .withStandardItems(true)
            .build()
        TestEntityUtils.setEntityId(testOrder, 1L)
        
        // 테스트용 OrderInfo 생성
        testOrderInfo = OrderInfo(
            id = 1L,
            userId = 1L,
            items = listOf(
                OrderItemInfo(
                    id = 1L,
                    productId = 1L,
                    variantId = 1L,
                    quantity = 2,
                    unitPrice = Money.of(10000),
                    discountAmount = Money.ZERO
                )
            ),
            originalTotal = Money.of(20000),
            discountedAmount = Money.ZERO
        )
        
        paymentCommand = OrderCommand.ProcessPayment.Root(
            orderId = 1L,
            pgPaymentId = "pg_12345",
            paymentMethod = "CARD",
            timestamp = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("결제 Saga 정상 프로세스 테스트")
    inner class SuccessfulPaymentSagaTest {
        
        @Test
        @DisplayName("✅ 모든 단계가 성공적으로 진행되어 주문이 완료된다")
        fun `결제 프로세스가 정상적으로 완료된다`() {
            // given
            val pointDeductResponse = DeductUserPointResponse(
                userId = 1L,
                deductedAmount = Money.of(20000),
                remainingBalance = Money.of(50000)
            )
            
            val paymentResponse = ProcessPaymentResponse(
                orderId = 1L,
                paymentId = 100L,
                pgPaymentId = "pg_12345",
                paidAmount = Money.of(20000),
                status = PaymentStatus.SUCCESS,
                paidAt = LocalDateTime.now(),
                message = "결제 완료"
            )
            
            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.success(pointDeductResponse)
            every { paymentClient.processPayment(any()) } returns Result.success(paymentResponse)
            every { orderService.saveOrder(any()) } returns testOrder
            every { orderService.completeOrder(any(), any()) } returns Result.success(mockk())
            every { eventPublisher.publish(any<OrderEvent.Completed>()) } just Runs
            
            // when
            val result = paymentSagaOrchestrator.executePaymentSaga(paymentCommand)
            
            // then
            verify(exactly = 1) { paymentClient.deductUserPoint(any()) }
            verify(exactly = 1) { paymentClient.processPayment(any()) }
            verify(exactly = 1) { orderService.completeOrder(any(), any()) }
        }
    }

    @Nested
    @DisplayName("결제 Saga 예외 케이스 및 보상 로직 테스트")
    inner class PaymentSagaCompensationTest {

        @Test
        @DisplayName("✅ 포인트 차감 실패 시 보상 로직이 실행되지 않는다")
        fun `포인트 차감 실패 시 보상 로직이 실행되지 않는다`() {
            // given
            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.failure(RuntimeException("포인트 잔액 부족"))
            every { orderService.fail(any()) } just Runs
            every { couponClient.getUsedCouponIdsByOrderId(any()) } returns emptyList()
            every { orderSagaOrchestrator.compensateOrderSaga(any(), any()) } just Runs
            every { eventPublisher.publish(any<OrderEvent.PaymentFailed>()) } just Runs

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            verify(exactly = 1) { paymentClient.deductUserPoint(any()) }
            verify(exactly = 0) { paymentClient.processPayment(any()) }
            verify(exactly = 0) { paymentClient.restoreUserPoint(any()) }
            verify(exactly = 1) { orderService.fail(any()) }
        }

        @Test
        @DisplayName("✅ 결제 처리 실패 시 포인트 복구 보상이 실행된다")
        fun `결제 처리 실패 시 포인트 복구 보상이 실행된다`() {
            // given
            val pointDeductResponse = DeductUserPointResponse(
                userId = 1L,
                deductedAmount = Money.of(20000),
                remainingBalance = Money.of(50000)
            )

            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.success(pointDeductResponse)
            every { paymentClient.processPayment(any()) } returns Result.failure(RuntimeException("결제 실패"))
            every { paymentClient.restoreUserPoint(any()) } returns Result.success(Unit)
            every { orderService.fail(any()) } just Runs
            every { couponClient.getUsedCouponIdsByOrderId(any()) } returns emptyList()
            every { orderSagaOrchestrator.compensateOrderSaga(any(), any()) } just Runs
            every { eventPublisher.publish(any<OrderEvent.PaymentFailed>()) } just Runs

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            verify(exactly = 1) { paymentClient.deductUserPoint(any()) }
            verify(exactly = 1) { paymentClient.processPayment(any()) }
            verify(exactly = 1) { paymentClient.restoreUserPoint(any()) }
            verify(exactly = 1) { orderService.fail(any()) }
        }

        @Test
        @DisplayName("✅ 주문 완료 처리 실패 시 모든 보상 로직이 실행된다")
        fun `주문 완료 처리 실패 시 모든 보상 로직이 실행된다`() {
            // given
            println("testOrder.status = ${testOrder.status}")

            val pointDeductResponse = DeductUserPointResponse(
                userId = 1L,
                deductedAmount = Money.of(20000),
                remainingBalance = Money.of(50000)
            )

            val paymentResponse = ProcessPaymentResponse(
                orderId = 1L,
                paymentId = 100L,
                pgPaymentId = "pg_12345",
                paidAmount = Money.of(20000),
                status = PaymentStatus.SUCCESS,
                paidAt = LocalDateTime.now(),
                message = "결제 완료"
            )

            val failPaymentResponse = FailPaymentResponse(
                orderId = 1L,
                paymentId = 100L,
                status = PaymentStatus.SUCCESS,
                failedAt = LocalDateTime.now(),
                message = "",
            )

            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.success(pointDeductResponse)
            every { paymentClient.processPayment(any()) } returns Result.success(paymentResponse)
            every { orderService.saveOrder(any()) } throws RuntimeException("주문 저장 실패")
            every { paymentClient.restoreUserPoint(any()) } returns Result.success(Unit)
            every { orderService.fail(any()) } just Runs
            every { couponClient.getUsedCouponIdsByOrderId(any()) } returns emptyList()
            every { orderSagaOrchestrator.compensateOrderSaga(any(), any()) } just Runs
            every { eventPublisher.publish(any<OrderEvent.PaymentFailed>()) } just Runs
            every { orderService.completeOrder(any(), any()) } returns Result.failure(RuntimeException("주문 완료 실패"))
            every { paymentClient.failPayment(any()) } returns Result.success(failPaymentResponse)

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            assertSoftly {
                verify(exactly = 1) { paymentClient.restoreUserPoint(any()) } // 포인트 복구 보상
                verify(exactly = 1) { paymentClient.failPayment(any()) } // 포인트 복구 보상
                verify(exactly = 1) { orderService.fail(any()) }
            }
        }

        @Test
        @DisplayName("✅ 포인트 복구 실패해도 전체 보상 프로세스는 계속 진행된다")
        fun `포인트 복구 실패해도 전체 보상 프로세스는 계속 진행된다`() {
            // given
            val pointDeductResponse = DeductUserPointResponse(
                userId = 1L,
                deductedAmount = Money.of(20000),
                remainingBalance = Money.of(50000)
            )

            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.success(pointDeductResponse)
            every { paymentClient.processPayment(any()) } returns Result.failure(RuntimeException("결제 실패"))
            every { paymentClient.restoreUserPoint(any()) } returns Result.failure(RuntimeException("포인트 복구 실패"))
            every { orderService.fail(any()) } just Runs
            every { couponClient.getUsedCouponIdsByOrderId(any()) } returns emptyList()
            every { orderSagaOrchestrator.compensateOrderSaga(any(), any()) } just Runs
            every { eventPublisher.publish(any<OrderEvent.PaymentFailed>()) } just Runs

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            verify(exactly = 1) { paymentClient.restoreUserPoint(any()) }
            verify(exactly = 1) { orderService.fail(any()) }
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    inner class ExceptionHandlingTest {
        
        @Test
        @DisplayName("✅ PaymentSagaException이 발생하면 적절히 처리된다")
        fun `PaymentSagaException이 발생하면 적절히 처리된다`() {
            // given
            every { orderService.getOrderInfoById(1L) } returns testOrderInfo
            every { orderService.getOrderById(1L) } returns testOrder
            every { paymentClient.deductUserPoint(any()) } returns Result.failure(RuntimeException("포인트 부족"))
            every { orderService.fail(any()) } just Runs
            every { couponClient.getUsedCouponIdsByOrderId(any()) } returns emptyList()
            every { orderSagaOrchestrator.compensateOrderSaga(any(), any()) } just Runs
            every { eventPublisher.publish(any<OrderEvent.PaymentFailed>()) } just Runs
            
            // when & then
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }
        }
    }
} 