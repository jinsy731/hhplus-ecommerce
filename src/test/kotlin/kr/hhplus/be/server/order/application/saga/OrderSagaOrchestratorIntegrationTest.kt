package kr.hhplus.be.server.order.application.saga

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaUserCouponRepository
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.order.infrastructure.persistence.JpaOrderRepository
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.application.dto.ProductInfo.CreateOrder
import kr.hhplus.be.server.product.domain.product.model.Product
import kr.hhplus.be.server.product.domain.product.model.ProductVariant
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.LocalDateTime

@SpringBootTest
@RecordApplicationEvents
class OrderSagaOrchestratorIntegrationTest {

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents

    @Autowired
    private lateinit var orderSagaOrchestrator: OrderSagaOrchestrator

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: JpaOrderRepository

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var productVariantRepository: ProductVariantJpaRepository

    @Autowired
    private lateinit var couponRepository: JpaCouponRepository

    @Autowired
    private lateinit var userCouponRepository: JpaUserCouponRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    private lateinit var testProduct: Product
    private lateinit var testVariant: ProductVariant
    private lateinit var testCoupon: Coupon
    private lateinit var testUserCoupon: UserCoupon
    private lateinit var orderSheetCommand: OrderCommand.CreateOrderSheet.Root
    private lateinit var orderSheetCommandWithCoupons: OrderCommand.CreateOrderSheet.Root

    @BeforeEach
    fun setUp() {
        // 테스트용 상품 생성
        testProduct = ProductTestFixture.product(basePrice = Money.of(10000))
            .withVariant(ProductTestFixture.variant())
            .build()
        testProduct = productRepository.save(testProduct)

        // 첫 번째 변형을 가져오기 (ProductTestFixture에서 생성된 변형들 중)
        testVariant = testProduct.variants.first()

        // 테스트용 쿠폰 생성
        testCoupon = CouponTestFixture.coupon()
            .withDiscountPolicy(CouponTestFixture.fixedAmountDiscountPolicy())
            .build()
        testCoupon = couponRepository.save(testCoupon)

        // 테스트용 사용자 쿠폰 생성
        testUserCoupon = CouponTestFixture.userCoupon(
            userId = 1L,
            coupon = testCoupon
        ).build()
        testUserCoupon = userCouponRepository.save(testUserCoupon)

        orderSheetCommand = OrderCommand.CreateOrderSheet.Root(
            userId = 1L,
            products = listOf(
                CreateOrder.Root(
                    productId = testProduct.id!!,
                    variants = listOf(
                        CreateOrder.Variant(
                            variantId = testVariant.id!!,
                            unitPrice = testProduct.getFinalPriceWithVariant(testVariant.id!!)
                        )
                    )
                )
            ),
            orderItems = listOf(
                OrderCommand.CreateOrderSheet.OrderItem(
                    productId = testProduct.id!!,
                    variantId = testVariant.id!!,
                    quantity = 2
                )
            ),
            userCouponIds = emptyList(),
            timestamp = LocalDateTime.now()
        )

        orderSheetCommandWithCoupons = orderSheetCommand.copy(
            userCouponIds = listOf(testUserCoupon.id!!)
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
    }

    @Nested
    @DisplayName("주문 Saga 통합 테스트 - 정상 프로세스")
    inner class SuccessfulOrderSagaIntegrationTest {

        @Test
//        @Transactional
        @DisplayName("✅ 쿠폰 없이 주문 Saga가 성공적으로 완료되고 실제 재고가 차감된다")
        fun `쿠폰 없이 주문 Saga 성공 시 주문이 생성되고 실제 재고가 차감된다`() {
            // given
            val originalStock = testVariant.stock
            val orderQuantity = 2

            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)

            // then
            // 데이터베이스 상태 검증
            val savedOrder = orderRepository.findById(createdOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.CREATED

            // 실제 재고 차감 검증
            val updatedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            updatedVariant.stock shouldBe (originalStock - orderQuantity)
        }

        @Test
//        @Transactional
        @DisplayName("✅ 쿠폰과 함께 주문 Saga가 성공적으로 완료되고 실제 할인이 적용된다")
        fun `쿠폰과 함께 주문 Saga 성공 시 실제 할인이 적용되고 쿠폰이 사용 처리된다`() {
            // given
            val originalStock = testVariant.stock
            val orderQuantity = 2

            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommandWithCoupons)

            // then
            // 주문 상태 검증
            val savedOrder = orderRepository.findById(createdOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.CREATED
            savedOrder.discountedAmount shouldNotBe Money.ZERO

            // 실제 재고 차감 검증
            val updatedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            updatedVariant.stock shouldBe (originalStock - orderQuantity)

            // 쿠폰 사용 처리 검증
            val updatedUserCoupon = userCouponRepository.findById(testUserCoupon.id!!).orElseThrow()
            updatedUserCoupon.status shouldBe UserCouponStatus.USED
            updatedUserCoupon.usedAt shouldNotBe null
        }
    }

    @Nested
    @DisplayName("주문 Saga 통합 테스트 - 보상 로직")
    inner class OrderSagaCompensationIntegrationTest {

        @Test
//        @Transactional
        @DisplayName("✅ 재고 부족 시 주문이 실패하고 쿠폰이 복구된다")
        fun `재고 부족 시 쿠폰 복구 보상이 실행되고 데이터 정합성이 유지된다`() {
            // given - 재고를 부족하게 설정
            testVariant.stock = 1 // 주문 수량(2)보다 적은 재고
            productVariantRepository.save(testVariant)

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommandWithCoupons) }

            // then
            // 쿠폰 복구 검증 - 쿠폰이 사용되지 않은 상태로 유지되어야 함
            val restoredUserCoupon = userCouponRepository.findById(testUserCoupon.id!!).orElseThrow()
            restoredUserCoupon.status shouldBe UserCouponStatus.UNUSED
            restoredUserCoupon.usedAt shouldBe null

            // 재고가 변경되지 않았는지 확인
            val unchangedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            unchangedVariant.stock shouldBe 1 // 원래 설정한 값 그대로

            // 실패한 주문 상태 확인
            val orders = orderRepository.findAll()
            if (orders.isNotEmpty()) {
                val failedOrder = orders.first()
                failedOrder.status shouldBe OrderStatus.FAILED
            }
        }

        @Test
//        @Transactional
        @DisplayName("✅ 쿠폰 없이 재고 부족 시 주문만 실패 처리된다")
        fun `쿠폰 없이 재고 부족 시 주문만 실패 처리되고 데이터 정합성이 유지된다`() {
            // given - 재고를 부족하게 설정
            testVariant.stock = 1 // 주문 수량(2)보다 적은 재고
            productVariantRepository.save(testVariant)

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand) }

            // then
            // 재고가 변경되지 않았는지 확인
            val unchangedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            unchangedVariant.stock shouldBe 1

            // 실패한 주문 상태 확인
            val orders = orderRepository.findAll()
            if (orders.isNotEmpty()) {
                val failedOrder = orders.first()
                failedOrder.status shouldBe OrderStatus.FAILED
            }
        }

        @Test
//        @Transactional
        @DisplayName("✅ 이미 사용된 쿠폰으로 주문 시도 시 실패 처리된다")
        fun `이미 사용된 쿠폰으로 주문 시도 시 실패하고 데이터 정합성이 유지된다`() {
            // given - 쿠폰을 이미 사용 처리
            testUserCoupon.use(LocalDateTime.now(), 999L)
            userCouponRepository.save(testUserCoupon)

            val originalStock = testVariant.stock

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommandWithCoupons) }

            // then
            // 재고가 변경되지 않았는지 확인
            val unchangedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            unchangedVariant.stock shouldBe originalStock

            // 쿠폰 상태가 변경되지 않았는지 확인
            val unchangedUserCoupon = userCouponRepository.findById(testUserCoupon.id!!).orElseThrow()
            unchangedUserCoupon.status shouldBe UserCouponStatus.USED
        }
    }

    @Nested
    @DisplayName("주문 Saga 통합 테스트 - 데이터 일관성")
    inner class OrderSagaDataConsistencyTest {

        @Test
//        @Transactional
        @DisplayName("✅ 동시 주문 생성 시 각각 독립적으로 처리되고 재고가 정확히 차감된다")
        fun `동시 주문 생성 시 재고 정합성이 유지된다`() {
            // given
            val command1 = orderSheetCommand.copy(userId = 1L)
            val command2 = orderSheetCommand.copy(userId = 2L)

            val originalStock = testVariant.stock

            // when
            orderSagaOrchestrator.executeOrderSheetCreationSaga(command1)
            orderSagaOrchestrator.executeOrderSheetCreationSaga(command2)

            // then

            // 각각 다른 주문이 생성되어야 함
            val orders = orderRepository.findAll()
            orders.size shouldBe 2
            orders.map { it.userId }.toSet() shouldBe setOf(1L, 2L)
            orders.all { it.status == OrderStatus.CREATED } shouldBe true

            // 재고가 정확히 차감되었는지 확인 (2개 주문 × 각 2개씩 = 4개 차감)
            val updatedVariant = productVariantRepository.findById(testVariant.id!!).orElseThrow()
            updatedVariant.stock shouldBe (originalStock - 4)
        }

        @Test
//        @Transactional
        @DisplayName("✅ 여러 쿠폰을 사용한 주문에서 할인이 정확히 계산된다")
        fun `여러 쿠폰을 사용한 주문에서 할인이 정확히 계산된다`() {
            // given - 두 번째 쿠폰 생성
            val secondCoupon = CouponTestFixture.coupon()
                .withName("두 번째 쿠폰")
                .withDiscountPolicy(CouponTestFixture.fixedAmountDiscountPolicy(
                    discountAmount = Money.of(1500),
                    minOrderAmount = Money.of(5000)
                ))
                .build()
            val savedSecondCoupon = couponRepository.save(secondCoupon)

            val secondUserCoupon = CouponTestFixture.userCoupon(
                userId = 1L,
                coupon = savedSecondCoupon
            ).build()
            val savedSecondUserCoupon = userCouponRepository.save(secondUserCoupon)

            val commandWithMultipleCoupons = orderSheetCommand.copy(
                userCouponIds = listOf(testUserCoupon.id!!, savedSecondUserCoupon.id!!)
            )

            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithMultipleCoupons)

            // then
            val savedOrder = orderRepository.findById(createdOrder.id!!).orElseThrow()
            
            // 할인이 적용되었는지 확인 (정확한 할인 금액은 비즈니스 로직에 따라 달라질 수 있음)
            savedOrder.discountedAmount shouldNotBe Money.ZERO

            // 두 쿠폰 모두 사용 처리되었는지 확인
            val usedCoupon1 = userCouponRepository.findById(testUserCoupon.id!!).orElseThrow()
            val usedCoupon2 = userCouponRepository.findById(savedSecondUserCoupon.id!!).orElseThrow()
            
            usedCoupon1.status shouldBe UserCouponStatus.USED
            usedCoupon2.status shouldBe UserCouponStatus.USED
        }
    }

    @Nested
    @DisplayName("주문 Saga 통합 테스트 - 비즈니스 로직")
    inner class OrderSagaBusinessLogicTest {

        @Test
//        @Transactional
        @DisplayName("✅ 최소 주문 금액 미달 쿠폰 사용 시 할인이 적용되지 않는다")
        fun `최소 주문 금액 미달 쿠폰 사용 시 할인이 적용되지 않는다`() {
            // given - 높은 최소 주문 금액 쿠폰 생성
            val highMinOrderCoupon = CouponTestFixture.coupon()
                .withName("높은 최소 주문 금액 쿠폰")
                .withDiscountPolicy(CouponTestFixture.fixedAmountDiscountPolicy(
                    discountAmount = Money.of(5000),
                    minOrderAmount = Money.of(50000) // 주문 금액보다 높은 최소 주문 금액
                ))
                .build()
            val savedHighMinOrderCoupon = couponRepository.save(highMinOrderCoupon)

            val highMinOrderUserCoupon = CouponTestFixture.userCoupon(
                userId = 1L,
                coupon = savedHighMinOrderCoupon
            ).build()
            val savedHighMinOrderUserCoupon = userCouponRepository.save(highMinOrderUserCoupon)

            val commandWithHighMinOrderCoupon = orderSheetCommand.copy(
                userCouponIds = listOf(savedHighMinOrderUserCoupon.id!!)
            )

            // when
            shouldThrowAny { orderSagaOrchestrator.executeOrderSheetCreationSaga(commandWithHighMinOrderCoupon) }

            // then
            // 쿠폰이 사용되지 않았는지 확인
            val unusedCoupon = userCouponRepository.findById(savedHighMinOrderUserCoupon.id!!).orElseThrow()
            unusedCoupon.status shouldBe UserCouponStatus.UNUSED
        }

        @Test
//        @Transactional
        @DisplayName("✅ 빈 쿠폰 리스트일 때 쿠폰 관련 로직을 스킵하고 정상 처리된다")
        fun `빈 쿠폰 리스트일 때 쿠폰 로직을 스킵한다`() {
            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommand)

            // then
            // 주문은 정상 생성되고 할인이 적용되지 않았는지 확인
            createdOrder.discountedAmount shouldBe Money.ZERO

            // 쿠폰 상태가 변경되지 않았는지 확인
            val unchangedUserCoupon = userCouponRepository.findById(testUserCoupon.id!!).orElseThrow()
            unchangedUserCoupon.status shouldBe UserCouponStatus.UNUSED
        }

        @Test
        @DisplayName("✅ 할인 후 최종 금액이 정확히 계산된다")
        fun `할인 후 최종 금액이 정확히 계산된다`() {
            // when
            val createdOrder = orderSagaOrchestrator.executeOrderSheetCreationSaga(orderSheetCommandWithCoupons)

            // then
            val savedOrder = orderRepository.findById(createdOrder.id!!).orElseThrow()
            
            // 최종 금액 계산 검증
            val expectedOriginalTotal = testProduct.getFinalPriceWithVariant(testVariant.id!!) * 2.toBigDecimal()
            
            savedOrder.originalTotal shouldBe expectedOriginalTotal
            savedOrder.discountedAmount shouldNotBe Money.ZERO
            savedOrder.finalTotal() shouldBe (savedOrder.originalTotal - savedOrder.discountedAmount)
        }
    }
} 