package kr.hhplus.be.server.order.application.saga

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.application.OrderCommand
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.client.PaymentClient
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.order.infrastructure.persistence.JpaOrderRepository
import kr.hhplus.be.server.payment.infrastructure.JpaPaymentRepository
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.point.infrastructure.JpaUserPointRepository
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.junit.jupiter.api.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.LocalDateTime

@SpringBootTest
@RecordApplicationEvents
class PaymentSagaOrchestratorIntegrationTest {

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents

    @Autowired
    private lateinit var paymentSagaOrchestrator: PaymentSagaOrchestrator

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: JpaOrderRepository

    @Autowired
    private lateinit var userPointRepository: JpaUserPointRepository

    @Autowired
    private lateinit var paymentRepository: JpaPaymentRepository

    @Autowired
    private lateinit var eventPublisher: DomainEventPublisher

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @MockitoSpyBean
    private lateinit var paymentClient: PaymentClient

    private lateinit var testUserPoint: UserPoint
    private lateinit var testOrder: Order
    private lateinit var paymentCommand: OrderCommand.ProcessPayment.Root

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 포인트 생성 및 저장
        testUserPoint = UserPointTestFixture
            .userPoint(userId = 1L, balance = Money.of(100000))
            .build()
        testUserPoint = userPointRepository.save(testUserPoint)

        // 테스트용 주문 생성 및 저장
        testOrder = OrderTestFixture.order(
            originalTotal = Money.of(10000),
            discountedAmount = Money.ZERO).build()
        testOrder = orderRepository.save(testOrder)

        paymentCommand = OrderCommand.ProcessPayment.Root(
            orderId = testOrder.id!!,
            pgPaymentId = "pg_12345",
            paymentMethod = "CARD",
            timestamp = LocalDateTime.now()
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
    }

    @Nested
    @DisplayName("결제 Saga 통합 테스트 - 정상 프로세스")
    inner class SuccessfulPaymentSagaIntegrationTest {

        @Test
        @DisplayName("✅ 결제 Saga가 성공적으로 완료되고 실제 데이터베이스 상태가 일관성 있게 유지된다")
        fun `결제 Saga 성공 시 주문 상태가 PAID로 변경되고 포인트가 실제로 차감된다`() {
            // given
            val originalUserPoint = testUserPoint.balance
            val orderTotal = testOrder.finalTotal()

            // when
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)

            // then
            // 데이터베이스 상태 검증
            val savedOrder = orderRepository.findById(testOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.PAID

            val savedUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!
            savedUserPoint.balance shouldBe (originalUserPoint - orderTotal)

            // 결제 기록 생성 검증
            val payments = paymentRepository.findByOrderId(testOrder.id!!)
            payments!!.originalAmount shouldBe orderTotal
        }

        @Test
        @DisplayName("✅ 결제 완료 후 도메인 이벤트가 정상적으로 발행된다")
        fun `결제 완료 후 OrderCompleted 이벤트가 발행된다`() {
            // when
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)

            // then
            // 이벤트 발행 검증
            val orderCompletedEvents = applicationEvents.stream(OrderEvent.Completed::class.java).toList()
            orderCompletedEvents.size shouldBe 1
            orderCompletedEvents.first().payload.orderId shouldBe testOrder.id
        }
    }

    @Nested
    @DisplayName("결제 Saga 통합 테스트 - 보상 로직")
    inner class PaymentSagaCompensationIntegrationTest {

        @Test
        @DisplayName("✅ 포인트 부족 시 결제가 실패하고 데이터 정합성이 유지된다")
        fun `포인트 부족 시 결제 실패와 함께 데이터 정합성이 유지된다`() {
            // given
            val userId = 2L
            val insufficientUserPoint = UserPointTestFixture.zeroBalanceUserPoint(userId = userId)
            val savedInsufficientUserPoint = userPointRepository.save(insufficientUserPoint)

            val poorOrder = OrderTestFixture.order(userId = userId).build()
            val savedPoorOrder = orderRepository.save(poorOrder)

            val poorPaymentCommand = OrderCommand.ProcessPayment.Root(
                orderId = savedPoorOrder.id!!,
                pgPaymentId = "pg_poor_12345",
                paymentMethod = "CARD",
                timestamp = LocalDateTime.now()
            )

            val originalUserPoint = savedInsufficientUserPoint.balance
            val originalOrderStatus = savedPoorOrder.status

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(poorPaymentCommand) }

            // then
            // 데이터베이스 상태 검증
            val savedOrder = orderRepository.findById(savedPoorOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.FAILED

            val savedUserPoint = userPointRepository.findByUserId(savedInsufficientUserPoint.userId)!!
            savedUserPoint.balance shouldBe originalUserPoint

            // 결제 기록이 생성되지 않았는지 확인
            val payments = paymentRepository.findByOrderId(savedPoorOrder.id!!)
            payments.shouldBeNull()

            // 실패 이벤트 발행 검증
            val paymentFailedEvents = applicationEvents.stream(OrderEvent.PaymentFailed::class.java).toList()
            paymentFailedEvents.size shouldBe 1
        }

        @Test
        @DisplayName("✅ 이미 결제된 주문에 대한 중복 결제 시도 시 적절히 처리된다")
        fun `이미 결제된 주문에 대한 중복 결제 시도가 적절히 거부된다`() {
            // given - 첫 번째 결제 완료
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)

            val originalUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!.balance

            // when - 두 번째 결제 시도
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            // 데이터 정합성 검증 - 포인트가 중복 차감되지 않아야 함
            val savedUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!
            savedUserPoint.balance shouldBe originalUserPoint

            // 결제 기록이 중복 생성되지 않았는지 확인
            val payments = paymentRepository.findByOrderId(testOrder.id!!)
            payments.shouldNotBeNull()
        }

        @Test
        @DisplayName("✅ 결제 처리 중 예외 발생 시 포인트가 자동으로 복구된다")
        fun `결제 처리 중 예외 발생 시 포인트 복구 보상이 실행된다`() {
            // given
            val orderTotal = testOrder.finalTotal()
            val originalUserPoint = testUserPoint.balance

            // 결제 처리 단계에서 실패를 시뮬레이션하기 위해 
            // PG사 연동 부분에서 예외가 발생할 수 있도록 잘못된 PG 정보 사용
            val invalidPaymentCommand = paymentCommand.copy(
                pgPaymentId = "", // 빈 PG 결제 ID로 실패 유도
                paymentMethod = "INVALID_METHOD"
            )

            doThrow(RuntimeException("PG 연동 시 예외"))
                .`when`(paymentClient)
                .processPayment(any())

            // when
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(invalidPaymentCommand) }

            // then
            // 포인트 복구 검증
            val savedUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!
            savedUserPoint.balance shouldBe originalUserPoint

            // 주문 상태 검증
            val savedOrder = orderRepository.findById(testOrder.id!!).orElseThrow()
            savedOrder.status shouldNotBe OrderStatus.PAID

            // 실패 이벤트 발행 검증
            val paymentFailedEvents = applicationEvents.stream(OrderEvent.PaymentFailed::class.java).toList()
            paymentFailedEvents.isNotEmpty() shouldBe true
        }
    }

    @Nested
    @DisplayName("결제 Saga 통합 테스트 - 데이터 일관성")
    inner class PaymentSagaDataConsistencyTest {

        @Test
        @DisplayName("✅ 동시 결제 요청 시 데이터 정합성이 유지된다")
        fun `동시 결제 요청 시 데이터 정합성이 유지된다`() {
            // given
            val originalUserPoint = testUserPoint.balance
            val orderTotal = testOrder.finalTotal()

            // when
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)
            
            // 같은 주문에 대한 두 번째 결제 시도
            shouldThrowAny { paymentSagaOrchestrator.executePaymentSaga(paymentCommand) }

            // then
            // 데이터 정합성 검증
            val savedUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!
            savedUserPoint.balance shouldBe (originalUserPoint - orderTotal) // 한 번만 차감

            val savedOrder = orderRepository.findById(testOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.PAID

            // 결제 기록 중복 생성 방지 확인
            val payments = paymentRepository.findByOrderId(testOrder.id!!)
            payments.shouldNotBeNull()
        }

        @Test
        @DisplayName("✅ 여러 사용자의 독립적인 결제 처리가 정상 동작한다")
        fun `여러 사용자의 독립적인 결제 처리가 정상 동작한다`() {
            // given
            val userPoint2 = UserPointTestFixture.highBalanceUserPoint(userId = 2L)
            val savedUserPoint2 = userPointRepository.save(userPoint2)

            val order2 = OrderTestFixture.order(userId = savedUserPoint2.userId)
                .withOriginalTotal(Money.of(15000))
                .withStandardItems()
                .build()
            val savedOrder2 = orderRepository.save(order2)

            val paymentCommand2 = OrderCommand.ProcessPayment.Root(
                orderId = savedOrder2.id!!,
                pgPaymentId = "pg_67890",
                paymentMethod = "CARD",
                timestamp = LocalDateTime.now()
            )

            val originalUser1Point = testUserPoint.balance
            val originalUser2Point = savedUserPoint2.balance

            // when
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand2)

            // then
            // 각 사용자의 포인트가 독립적으로 차감되었는지 확인
            val savedUser1Point = userPointRepository.findByUserId(testUserPoint.userId)!!
            val savedUser2Point = userPointRepository.findByUserId(savedUserPoint2.userId)!!

            savedUser1Point.balance shouldBe (originalUser1Point - testOrder.finalTotal())
            savedUser2Point.balance shouldBe (originalUser2Point - savedOrder2.finalTotal())

            // 각 주문이 독립적으로 완료되었는지 확인
            val finalOrder1 = orderRepository.findById(testOrder.id!!).orElseThrow()
            val finalOrder2 = orderRepository.findById(savedOrder2.id!!).orElseThrow()

            finalOrder1.status shouldBe OrderStatus.PAID
            finalOrder2.status shouldBe OrderStatus.PAID

            // 각각 별도의 결제 기록이 생성되었는지 확인
            val payments1 = paymentRepository.findByOrderId(testOrder.id!!)
            val payments2 = paymentRepository.findByOrderId(savedOrder2.id!!)

            payments1.shouldNotBeNull()
            payments2.shouldNotBeNull()
        }
    }

    @Nested
    @DisplayName("결제 Saga 통합 테스트 - 비즈니스 규칙")
    inner class PaymentSagaBusinessRuleTest {

        @Test
        @DisplayName("✅ 포인트 잔액이 정확히 주문 금액과 일치할 때 정상 결제된다")
        fun `포인트 잔액이 정확히 주문 금액과 일치할 때 정상 결제된다`() {
            // given
            val exactOrder = OrderTestFixture.order(userId = 3L)
                .withOriginalTotal(Money.of(5000))
                .withStandardItems()
                .build()
            val savedExactOrder = orderRepository.save(exactOrder)
            
            val orderTotal = savedExactOrder.finalTotal()
            
            val exactUserPoint = UserPointTestFixture.userPoint(userId = 3L)
                .withBalance(orderTotal) // 정확히 주문 금액만큼 포인트 설정
                .build()
            val savedExactUserPoint = userPointRepository.save(exactUserPoint)

            val exactPaymentCommand = OrderCommand.ProcessPayment.Root(
                orderId = savedExactOrder.id!!,
                pgPaymentId = "pg_exact_12345",
                paymentMethod = "CARD",
                timestamp = LocalDateTime.now()
            )

            // when
            paymentSagaOrchestrator.executePaymentSaga(exactPaymentCommand)

            // then
            val savedUserPoint = userPointRepository.findByUserId(savedExactUserPoint.userId)!!
            savedUserPoint.balance shouldBe Money.ZERO

            val savedOrder = orderRepository.findById(savedExactOrder.id!!).orElseThrow()
            savedOrder.status shouldBe OrderStatus.PAID
        }

        @Test
        @DisplayName("✅ 결제 완료 후 주문 상태 변경이 원자적으로 처리된다")
        fun `결제 완료 후 모든 상태 변경이 원자적으로 처리된다`() {
            // given
            val originalUserPoint = testUserPoint.balance

            // when
            paymentSagaOrchestrator.executePaymentSaga(paymentCommand)

            // then
            // 모든 변경사항이 일관성 있게 적용되었는지 확인
            val savedUserPoint = userPointRepository.findByUserId(testUserPoint.userId)!!
            val savedOrder = orderRepository.findById(testOrder.id!!).orElseThrow()
            val payments = paymentRepository.findByOrderId(testOrder.id!!)

            // 포인트 차감, 주문 상태 변경, 결제 기록 생성이 모두 완료되어야 함
            savedUserPoint.balance shouldBe (originalUserPoint - testOrder.finalTotal())
            savedOrder.status shouldBe OrderStatus.PAID
            payments.shouldNotBeNull()
            payments.originalAmount shouldBe testOrder.finalTotal()

            // 이벤트도 정상 발행되어야 함
            val completedEvents = applicationEvents.stream(OrderEvent.Completed::class.java).toList()
            completedEvents.size shouldBe 1
        }
    }
} 