package kr.hhplus.be.server.order.application.saga

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.*
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderInfo
import kr.hhplus.be.server.order.application.OrderItemInfo
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.client.*
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.shared.TestEntityUtils
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OrderSagaOrchestratorTest {

    private lateinit var orderSagaOrchestrator: OrderSagaOrchestrator
    private lateinit var orderService: OrderService
    private lateinit var couponClient: CouponClient
    private lateinit var productClient: ProductClient
    
    private lateinit var testOrder: Order
    private lateinit var testOrderInfo: OrderInfo
    private lateinit var orderSheetCommand: OrderCommand.CreateOrderSheet.Root
    
    @BeforeEach
    fun setUp() {
        orderService = mockk()
        couponClient = mockk()
        productClient = mockk()
        
        orderSagaOrchestrator = OrderSagaOrchestrator(
            orderService = orderService,
            couponClient = couponClient,
            productClient = productClient
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
        
        orderSheetCommand = OrderCommand.CreateOrderSheet.Root(
            userId = 1L,
            products = listOf(
                ProductInfo.CreateOrder.Root(
                    productId = 1L,
                    variants = listOf(
                        ProductInfo.CreateOrder.Variant(
                            variantId = 1L,
                            unitPrice = Money.of(10000)
                        )
                    )
                )
            ),
            orderItems = listOf(
                OrderCommand.CreateOrderSheet.OrderItem(
                    productId = 1L,
                    variantId = 1L,
                    quantity = 2
                )
            ),
            userCouponIds = emptyList(),
            timestamp = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("주문 Saga 정상 프로세스 테스트")
    inner class SuccessfulOrderSagaTest {
        
        @Test
        @DisplayName("✅ 쿠폰 없이 모든 단계가 성공적으로 진행되어 주문서가 생성된다")
        fun `쿠폰 없이 주문서 생성 프로세스가 정상적으로 완료된다`() {
            // given
            val stockResponse = ReduceStockResponse(
                orderId = 1L,
                processedItems = listOf(
                    ProcessedStockItem(
                        productId = 1L,
                        requestedQuantity = 2,
                        processedQuantity = 2
                    )
                )
            )
            
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { productClient.validateAndReduceStock(any()) } returns Result.success(stockResponse)
            
            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)
            
            // then
            createdOrder.status shouldBe OrderStatus.CREATED
            
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 2) { orderService.getOrderInfoById(any()) } // 쿠폰 적용 후, 재고 차감 전 2번 호출
            verify(exactly = 0) { couponClient.useCoupons(any()) } // 쿠폰이 없으므로 호출되지 않음
            verify(exactly = 1) { productClient.validateAndReduceStock(any()) }
        }
        
        @Test
        @DisplayName("✅ 쿠폰과 함께 모든 단계가 성공적으로 진행되어 주문서가 생성된다")
        fun `쿠폰과 함께 주문서 생성 프로세스가 정상적으로 완료된다`() {
            // given
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L, 2L))
            
            val couponResponse = UseCouponResponse(
                orderId = 1L,
                appliedDiscounts = listOf(
                    AppliedDiscount(
                        orderItemId = 1L,
                        sourceId = 1L,
                        discountAmount = 1000L,
                        discountType = DiscountType.FIXED_AMOUNT
                    )
                )
            )
            
            val stockResponse = ReduceStockResponse(
                orderId = 1L,
                processedItems = listOf(
                    ProcessedStockItem(
                        productId = 1L,
                        requestedQuantity = 2,
                        processedQuantity = 2
                    )
                )
            )
            
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.success(couponResponse)
            every { orderService.applyDiscountToOrder(any(), any()) } just Runs
            every { productClient.validateAndReduceStock(any()) } returns Result.success(stockResponse)
            
            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons)
            
            // then
            createdOrder.status shouldBe OrderStatus.CREATED
            
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 2) { orderService.getOrderInfoById(any()) } // 쿠폰 적용 후, 재고 차감 전 2번 호출
            verify(exactly = 1) { couponClient.useCoupons(any()) }
            verify(exactly = 1) { orderService.applyDiscountToOrder(any(), any()) }
            verify(exactly = 1) { productClient.validateAndReduceStock(any()) }
        }
    }

    @Nested
    @DisplayName("주문 Saga 예외 케이스 및 보상 로직 테스트")
    inner class OrderSagaCompensationTest {

        @Test
        @DisplayName("✅ 주문서 생성 실패 시 보상 로직이 실행되지 않는다")
        fun `주문서 생성 실패 시 보상 로직이 실행되지 않는다`() {
            // given
            every { orderService.createOrderSheet(any()) } throws RuntimeException("주문서 생성 실패")

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand) }

            // then
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 0) { couponClient.useCoupons(any()) }
            verify(exactly = 0) { productClient.validateAndReduceStock(any()) }
            verify(exactly = 0) { orderService.fail(any()) }
            verify(exactly = 0) { couponClient.restoreCoupons(any()) }
            verify(exactly = 0) { productClient.restoreStock(any()) }
        }

        @Test
        @DisplayName("✅ 쿠폰 적용 실패 시 주문 생성 보상이 실행된다")
        fun `쿠폰 적용 실패 시 주문 생성 보상이 실행된다`() {
            // given
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L, 2L))

            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.failure(RuntimeException("쿠폰 적용 실패"))
            every { orderService.fail(any()) } just Runs

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons) }

            // then
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 1) { orderService.getOrderInfoById(any()) }
            verify(exactly = 1) { couponClient.useCoupons(any()) }
            verify(exactly = 0) { productClient.validateAndReduceStock(any()) }
            verify(exactly = 1) { orderService.fail(any()) } // 주문 생성 보상
            verify(exactly = 0) { couponClient.restoreCoupons(any()) } // 쿠폰 적용 자체가 실패했으므로 복구 불필요
            verify(exactly = 0) { productClient.restoreStock(any()) }
        }

        @Test
        @DisplayName("✅ 재고 차감 실패 시 쿠폰과 주문 생성 보상이 실행된다")
        fun `재고 차감 실패 시 쿠폰과 주문 생성 보상이 실행된다`() {
            // given
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L, 2L))

            val couponResponse = UseCouponResponse(
                orderId = 1L,
                appliedDiscounts = listOf(
                    AppliedDiscount(
                        orderItemId = 1L,
                        sourceId = 1L,
                        discountAmount = 1000L,
                        discountType = DiscountType.FIXED_AMOUNT
                    )
                )
            )

            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.success(couponResponse)
            every { orderService.applyDiscountToOrder(any(), any()) } just Runs
            every { productClient.validateAndReduceStock(any()) } returns Result.failure(RuntimeException("재고 부족"))
            every { orderService.fail(any()) } just Runs
            every { couponClient.restoreCoupons(any()) } returns Result.success(Unit)

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons) }

            // then
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 2) { orderService.getOrderInfoById(any()) }
            verify(exactly = 1) { couponClient.useCoupons(any()) }
            verify(exactly = 1) { orderService.applyDiscountToOrder(any(), any()) }
            verify(exactly = 1) { productClient.validateAndReduceStock(any()) }
            verify(exactly = 1) { orderService.fail(any()) } // 주문 생성 보상
            verify(exactly = 1) { couponClient.restoreCoupons(any()) } // 쿠폰 보상
            verify(exactly = 0) { productClient.restoreStock(any()) } // 재고 차감이 실패했으므로 복구 불필요
        }

        @Test
        @DisplayName("✅ 쿠폰 없이 재고 차감 실패 시 주문 생성 보상만 실행된다")
        fun `쿠폰 없이 재고 차감 실패 시 주문 생성 보상만 실행된다`() {
            // given
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { productClient.validateAndReduceStock(any()) } returns Result.failure(RuntimeException("재고 부족"))
            every { orderService.fail(any()) } just Runs

            // when
            shouldThrowAny {
                orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)
            }

            // then
            verify(exactly = 1) { orderService.createOrderSheet(any()) }
            verify(exactly = 2) { orderService.getOrderInfoById(any()) }
            verify(exactly = 0) { couponClient.useCoupons(any()) } // 쿠폰이 없으므로 호출되지 않음
            verify(exactly = 1) { productClient.validateAndReduceStock(any()) }
            verify(exactly = 1) { orderService.fail(any()) } // 주문 생성 보상
            verify(exactly = 0) { productClient.restoreStock(any()) } // 재고 차감이 실패했으므로 복구 불필요
        }

        @Test
        @DisplayName("✅ 보상 로직 실패해도 전체 보상 프로세스는 계속 진행된다")
        fun `보상 로직 실패해도 전체 보상 프로세스는 계속 진행된다`() {
            // given
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L, 2L))

            val couponResponse = UseCouponResponse(
                orderId = 1L,
                appliedDiscounts = listOf(
                    AppliedDiscount(
                        orderItemId = 1L,
                        sourceId = 1L,
                        discountAmount = 1000L,
                        discountType = DiscountType.FIXED_AMOUNT
                    )
                )
            )

            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.success(couponResponse)
            every { orderService.applyDiscountToOrder(any(), any()) } just Runs
            every { productClient.validateAndReduceStock(any()) } returns Result.failure(RuntimeException("재고 부족"))
            every { orderService.fail(any()) } just Runs
            every { couponClient.restoreCoupons(any()) } returns Result.failure(RuntimeException("쿠폰 복구 실패"))

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons) }

            // then
            verify(exactly = 1) { couponClient.restoreCoupons(any()) }
            verify(exactly = 1) { orderService.fail(any()) }
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    inner class ExceptionHandlingTest {
        
        @Test
        @DisplayName("✅ OrderSagaException이 발생하면 적절히 처리된다")
        fun `OrderSagaException이 발생하면 적절히 처리된다`() {
            // given
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.failure(RuntimeException("쿠폰 서비스 오류"))
            every { orderService.fail(any()) } just Runs
            
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L))
            
            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons) }
            
            // then
            verify(exactly = 1) { orderService.fail(any()) }
        }
    }

    @Nested
    @DisplayName("주문서 생성 결과 검증 테스트")
    inner class OrderSheetCreationResultValidationTest {
        
        @Test
        @DisplayName("✅ 생성된 주문서의 상태가 올바르게 설정된다")
        fun `생성된 주문서의 상태가 올바르게 설정된다`() {
            // given
            val stockResponse = ReduceStockResponse(
                orderId = 1L,
                processedItems = listOf(
                    ProcessedStockItem(
                        productId = 1L,
                        requestedQuantity = 2,
                        processedQuantity = 2
                    )
                )
            )
            
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { productClient.validateAndReduceStock(any()) } returns Result.success(stockResponse)
            
            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)
            
            // then
            createdOrder.status shouldBe OrderStatus.CREATED
            createdOrder.userId shouldBe 1L
        }
        
        @Test
        @DisplayName("✅ 쿠폰 적용된 주문서의 할인이 정상적으로 처리된다")
        fun `쿠폰 적용된 주문서의 할인이 정상적으로 처리된다`() {
            // given
            val commandWithCoupons = orderSheetCommand.copy(userCouponIds = listOf(1L, 2L))
            
            val couponResponse = UseCouponResponse(
                orderId = 1L,
                appliedDiscounts = listOf(
                    AppliedDiscount(
                        orderItemId = 1L,
                        sourceId = 1L,
                        discountAmount = 1000L,
                        discountType = DiscountType.FIXED_AMOUNT
                    )
                )
            )
            
            val stockResponse = ReduceStockResponse(
                orderId = 1L,
                processedItems = listOf(
                    ProcessedStockItem(
                        productId = 1L,
                        requestedQuantity = 2,
                        processedQuantity = 2
                    )
                )
            )
            
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { orderService.getOrderInfoById(any()) } returns testOrderInfo
            every { couponClient.useCoupons(any()) } returns Result.success(couponResponse)
            every { orderService.applyDiscountToOrder(any(), any()) } just Runs
            every { productClient.validateAndReduceStock(any()) } returns Result.success(stockResponse)
            
            // when
            orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithCoupons)
            
            // then
            verify(exactly = 1) { orderService.applyDiscountToOrder(any(), any()) }
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 검증 테스트")
    inner class BusinessLogicValidationTest {
        
        @Test
        @DisplayName("✅ 빈 쿠폰 리스트일 때 쿠폰 적용을 스킵한다")
        fun `빈 쿠폰 리스트일 때 쿠폰 적용을 스킵한다`() {
            // given
            val stockResponse = ReduceStockResponse(
                orderId = 1L,
                processedItems = listOf(
                    ProcessedStockItem(
                        productId = 1L,
                        requestedQuantity = 2,
                        processedQuantity = 2
                    )
                )
            )

            every { orderService.getOrderInfoById(any())} returns testOrderInfo
            every { orderService.createOrderSheet(any()) } returns testOrder
            every { productClient.validateAndReduceStock(any()) } returns Result.success(stockResponse)
            
            // when
            orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)
            
            // then
            verify(exactly = 0) { couponClient.useCoupons(any()) }
            verify(exactly = 0) { orderService.applyDiscountToOrder(any(), any()) }
        }
    }
} 