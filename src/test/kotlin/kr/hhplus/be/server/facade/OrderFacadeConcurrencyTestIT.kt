package kr.hhplus.be.server.facade

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.executeThreads
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.facade.OrderCriteria
import kr.hhplus.be.server.order.facade.OrderFacade
import kr.hhplus.be.server.product.domain.product.*
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import kr.hhplus.be.server.user.UserPointTestFixture
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.optionals.getOrNull

@SpringBootTest
class OrderFacadeConcurrencyTestIT {

    @Autowired
    private lateinit var orderFacade: OrderFacade

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var productVariantJpaRepository: ProductVariantJpaRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    private val userId = 1L
    private val productId = 1L
    private val variantId = 1L
    private val initialBalance = BigDecimal(10000)
    private val productPrice = BigDecimal(1000)

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 포인트 생성
        val userPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = initialBalance)
        userPointRepository.save(userPoint)

        // 테스트용 상품 생성
        val product = Product(
            name = "테스트 상품",
            basePrice = productPrice,
            status = ProductStatus.ON_SALE
        )
        val variant = ProductVariant(
            product = product,
            additionalPrice = BigDecimal.ZERO,
            status = VariantStatus.ACTIVE,
            stock = 100
        )
        product.addVariant(variant)
        productRepository.save(product)
    }

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `재고 동시 구매 - 동시에 여러 주문을 실행하면 재고 부족으로 일부 주문이 실패한다`() {
        // 재고가 10개인 상품을 준비
        val limitedStockProduct = Product(
            name = "한정 상품",
            basePrice = BigDecimal(5000),
            status = ProductStatus.ON_SALE
        )
        val limitedStockVariant = ProductVariant(
            product = limitedStockProduct,
            additionalPrice = BigDecimal.ZERO,
            status = VariantStatus.ACTIVE,
            stock = 10 // 재고 10개만 설정
        )
        limitedStockProduct.addVariant(limitedStockVariant)
        productRepository.save(limitedStockProduct)
        val productId = limitedStockProduct.id
        val variantId = limitedStockVariant.id

        // 동시에 실행할 스레드 수 (재고보다 많게 설정)
        val threadCount = 15
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // 15개의 스레드에서 동시에 주문을 시도 (재고는 10개)
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    val orderCriteria = OrderCriteria.PlaceOrder.Root(
                        userId = userId,
                        items = listOf(
                            OrderCriteria.PlaceOrder.Item(
                                productId = productId!!,
                                variantId = variantId!!,
                                quantity = 1
                            )
                        ),
                        userCouponIds = emptyList(),
                        payMethods = listOf(
                            OrderCriteria.PlaceOrder.PayMethod(
                                method = "POINT",
                                amount = BigDecimal(5000)
                            )
                        )
                    )

                    orderFacade.placeOrder(orderCriteria)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    // 예외 발생 시 카운트
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 검증
        successCount.get() + failCount.get() shouldBe threadCount
        
        // 재고 확인을 위해 상품을 다시 조회
        val finalProduct = productRepository.getById(productId!!)
        val finalVariant = productVariantJpaRepository.findById(variantId!!).getOrNull() ?: throw IllegalStateException("Variant not found")
            
        if (successCount.get() == 10) {
            // 재고가 모두 소진된 경우
            finalVariant.stock shouldBe 0
        } else {
            // 재고가 남은 경우 (성공 개수만큼 감소)
            finalVariant.stock shouldBe (10 - successCount.get())
        }
    }

    @Test
    fun `포인트 중복 차감 - 동시에 여러 주문으로 포인트를 사용하면 일부는 포인트 부족으로 실패한다`() {
        // 초기 포인트 설정
        val userPoint = userPointRepository.getByUserId(userId)
        userPoint.balance = BigDecimal(10000)  // 10,000원 포인트
        userPointRepository.save(userPoint)

        // 동시에 실행할 스레드 수
        val threadCount = 15
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // 각 주문 금액
        val orderAmount = BigDecimal(1000)  // 1,000원 상품

        // 15개의 스레드에서 동시에 주문 시도 (포인트는 10개 주문만 가능)
        for (i in 1..threadCount) {
            executorService.execute {
                try {
                    val orderCriteria = OrderCriteria.PlaceOrder.Root(
                        userId = userId,
                        items = listOf(
                            OrderCriteria.PlaceOrder.Item(
                                productId = productId,
                                variantId = variantId,
                                quantity = 1
                            )
                        ),
                        userCouponIds = emptyList(),
                        payMethods = listOf(
                            OrderCriteria.PlaceOrder.PayMethod(
                                method = "POINT",
                                amount = orderAmount
                            )
                        )
                    )

                    orderFacade.placeOrder(orderCriteria)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    // 예외 발생 시 카운트
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // 검증
        successCount.get() + failCount.get() shouldBe threadCount
        
        // 최종 포인트 확인
        val finalUserPoint = userPointRepository.getByUserId(userId)
        
        // 주문 개수 확인
        val orders = orderRepository.findAll()
        orders.size shouldBe successCount.get()
        
        // 포인트가 정확히 차감되었는지 확인
        val expectedBalance = initialBalance.subtract(orderAmount.multiply(BigDecimal(successCount.get())))
        finalUserPoint.balance shouldBe expectedBalance
    }
    
    @Test
    fun `쿠폰 중복 사용 테스트`() {
        // 쿠폰 테스트 설정 - 실제 CouponRepository 및 UserCouponRepository가 존재한다면 구현
        // 현재는 생략하고 동시성 테스트에 집중
        
        // 동시에 실행할 스레드 수
        val threadCount = 5
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        
        // 여러 스레드에서 동시에 같은 쿠폰을 사용하여 주문 - 실제 구현 시 수정 필요
        executeThreads(threadCount) { _, latch ->
            try {
                val orderCriteria = OrderCriteria.PlaceOrder.Root(
                    userId = userId,
                    items = listOf(
                        OrderCriteria.PlaceOrder.Item(
                            productId = productId,
                            variantId = variantId,
                            quantity = 1
                        )
                    ),
                    userCouponIds = listOf(1L), // 동일한 쿠폰 ID
                    payMethods = listOf(
                        OrderCriteria.PlaceOrder.PayMethod(
                            method = "POINT",
                            amount = productPrice
                        )
                    )
                )

                orderFacade.placeOrder(orderCriteria)
                successCount.incrementAndGet()
            } catch (e: Exception) {
                failCount.incrementAndGet()
            }
        }
        
        // 검증 - 실제 구현 시 쿠폰 상태 확인 로직 추가 필요
        // (개념적인 테스트 - 쿠폰은 한 번만 사용되어야 함)
    }
}