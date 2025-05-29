package kr.hhplus.be.server.order.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.executeConcurrently
import kr.hhplus.be.server.order.application.saga.OrderSagaOrchestrator
import kr.hhplus.be.server.order.application.saga.PaymentSagaOrchestrator
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.infrastructure.JpaUserPointRepository
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.product.domain.product.model.ProductRepository
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import kr.hhplus.be.server.shared.domain.Money
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
class OrderSagaConcurrencyTestIT @Autowired constructor(
    private val orderSagaOrchestrator: OrderSagaOrchestrator,
    private val paymentSagaOrchestrator: PaymentSagaOrchestrator,
    private val jpaUserPointRepository: JpaUserPointRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val productVariantJpaRepository: ProductVariantJpaRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @AfterEach
    fun tearDown() = databaseCleaner.clean()

    @Test
    fun `✅Saga 동시성 테스트_주문서 생성과 결제가 동시에 발생할 때 동시성 문제없이 정확히 처리되어야 한다`() {
        // arrange: 재고가 100개인 옵션을 가진 상품과 유저 포인트 준비
        val succeededOrderIds = ConcurrentLinkedQueue<Long>()
        val failedCount = ConcurrentLinkedQueue<Exception>()
        
        val userPoints = IntRange(1, 120).map {
            logger.info { "UserPoint userId=$it" }
            UserPointTestFixture.userPoint(userId = it.toLong(), balance = Money.of(1500)).build()
        }
        val product = ProductTestFixture.product()
            .withVariants(ProductTestFixture.variant(stock = 100))
            .build()

        jpaUserPointRepository.saveAll(userPoints)
        val savedProduct = productRepository.save(product)
        val variant = productVariantJpaRepository.findByProductId(savedProduct.id!!)!!

        // act: 120개의 동시 주문서 생성 및 결제 요청
        executeConcurrently(120) { index ->
            try {
                val userId = (index + 1).toLong()
                val timestamp = LocalDateTime.now()
                
                // 1. 주문서 생성 Command 준비
                val createOrderSheetCmd = OrderCommand.CreateOrderSheet.Root(
                    userId = userId,
                    products = listOf(
                        ProductInfo.CreateOrder.Root(
                            productId = savedProduct.id!!,
                            variants = listOf(
                                ProductInfo.CreateOrder.Variant(
                                    variantId = variant.id!!,
                                    unitPrice = savedProduct.basePrice + variant.additionalPrice
                                )
                            )
                        )
                    ),
                    orderItems = listOf(
                        OrderCommand.CreateOrderSheet.OrderItem(
                            productId = savedProduct.id!!,
                            variantId = variant.id!!,
                            quantity = 1
                        )
                    ),
                    userCouponIds = listOf(),
                    timestamp = timestamp
                )

                // 2. OrderSagaOrchestrator를 통한 주문서 생성
                val orderSheet = orderSagaOrchestrator.executeOrderSheetCreationSaga(createOrderSheetCmd)

                // 3. PaymentSagaOrchestrator를 통한 결제 처리
                val processPaymentCmd = OrderCommand.ProcessPayment.Root(
                    orderId = orderSheet.id!!,
                    pgPaymentId = "pg_${orderSheet.id}",
                    paymentMethod = "POINT",
                    timestamp = timestamp
                )
                
                val completedOrder = paymentSagaOrchestrator.executePaymentSaga(processPaymentCmd)

                succeededOrderIds.add(completedOrder.id!!)
                logger.info { "Successfully processed order for userId: $userId, orderId: ${completedOrder.id}" }
                
            } catch (e: Exception) {
                failedCount.add(e)
                logger.error {"Failed to process order for userId: ${index + 1}, error: ${e.message}"}
            }
        }

        // assert: 정확히 100개의 주문만 성공하고, 재고와 포인트가 정확히 관리되어야 한다
        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val findProduct = productRepository.getById(savedProduct.id!!)
            val findVariant = productVariantJpaRepository.findByProductId(findProduct.id!!)!!
            val findUserPoints = jpaUserPointRepository.findAll()
            val orders = orderRepository.findAll()

            // 재고는 0이어야 함
            findVariant.stock shouldBe 0
            
            // 성공한 주문은 100개, 실패한 주문은 20개
            val paidOrders = orders.filter { it.status == OrderStatus.PAID }
            val failedOrders = orders.filter { it.status == OrderStatus.FAILED }

            val statusMap = orders.map { it.status }
            println("statusMap = ${statusMap}")
            
            paidOrders shouldHaveSize 100
            failedOrders shouldHaveSize 20
            
            // 포인트 잔액 검증: 100명은 잔액이 0, 20명은 잔액이 1500
            val zeroBalanceUsers = findUserPoints.filter { it.balance == Money.ZERO }
            val originalBalanceUsers = findUserPoints.filter { it.balance == Money.of(1500) }
            
            zeroBalanceUsers shouldHaveSize 100
            originalBalanceUsers shouldHaveSize 20
            
            logger.info { "Test completed - Succeeded orders: ${paidOrders.size}, Failed orders: ${failedOrders.size}" }
            logger.info { "Stock remaining: ${findVariant.stock}" }
            logger.info { "Users with zero balance: ${zeroBalanceUsers.size}, Users with original balance: ${originalBalanceUsers.size}" }
        }
    }

    @Test
    fun `✅Saga 보상 트랜잭션 테스트_결제 실패 시 재고와 쿠폰이 정확히 복구되어야 한다`() {
        // arrange: 재고 10개, 유저 포인트 부족 상황 생성
        val product = ProductTestFixture.product()
            .withVariants(ProductTestFixture.variant(stock = 10))
            .build()
        
        // 유저 포인트를 의도적으로 부족하게 설정
        val userPoint = UserPointTestFixture.userPoint(userId = 1L, balance = Money.of(100)).build()
        
        jpaUserPointRepository.save(userPoint)
        val savedProduct = productRepository.save(product)
        val variant = productVariantJpaRepository.findByProductId(savedProduct.id!!)!!
        val initialStock = variant.stock

        // act: 포인트 부족으로 결제 실패 상황 생성
        try {
            val timestamp = LocalDateTime.now()
            
            // 1. 주문서 생성 (성공)
            val createOrderSheetCmd = OrderCommand.CreateOrderSheet.Root(
                userId = 1L,
                products = listOf(
                    ProductInfo.CreateOrder.Root(
                        productId = savedProduct.id!!,
                        variants = listOf(
                            ProductInfo.CreateOrder.Variant(
                                variantId = variant.id!!,
                                unitPrice = savedProduct.basePrice + variant.additionalPrice
                            )
                        )
                    )
                ),
                orderItems = listOf(
                    OrderCommand.CreateOrderSheet.OrderItem(
                        productId = savedProduct.id!!,
                        variantId = variant.id!!,
                        quantity = 1
                    )
                ),
                userCouponIds = listOf(),
                timestamp = timestamp
            )

            val orderSheet = orderSagaOrchestrator.executeOrderSheetCreationSaga(createOrderSheetCmd)

            // 2. 결제 처리 (실패 예상 - 포인트 부족)
            val processPaymentCmd = OrderCommand.ProcessPayment.Root(
                orderId = orderSheet.id!!,
                pgPaymentId = "pg_${orderSheet.id}",
                paymentMethod = "POINT",
                timestamp = timestamp
            )
            
            paymentSagaOrchestrator.executePaymentSaga(processPaymentCmd)

        } catch (e: Exception) {
            logger.info { "Expected failure occurred: ${e.message}" }
        }

        // assert: 보상 트랜잭션이 정확히 동작하여 재고가 복구되어야 함
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            val findVariant = productVariantJpaRepository.findByProductId(savedProduct.id!!)!!
            val orders = orderRepository.findAll()
            val findUserPoint = jpaUserPointRepository.findByUserId(1L)!!

            // 재고가 원래대로 복구되어야 함
            findVariant.stock shouldBe initialStock
            
            // 주문 상태가 실패로 변경되어야 함
            val failedOrders = orders.filter { it.status == OrderStatus.FAILED }
            failedOrders shouldHaveSize 1
            
            // 유저 포인트는 원래 잔액 유지
            findUserPoint.balance shouldBe Money.of(100)
            
            logger.info { "Compensation test completed - Stock restored to: ${findVariant.stock}" }
        }
    }
}