package kr.hhplus.be.server.order.application

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.OrderTestFixture
import kr.hhplus.be.server.order.domain.OrderEventPublisher
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.shared.TestEntityUtils
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class OrderServiceTest {
    private lateinit var orderService: OrderService
    private lateinit var orderRepository: OrderRepository
    private lateinit var eventPublisher: OrderEventPublisher
    private lateinit var testOrder: Order

    @BeforeEach
    fun setup() {
        orderRepository = mock()
        eventPublisher = mock()
        orderService = OrderService(orderRepository, eventPublisher)

        testOrder = OrderTestFixture.order().withStandardItems(true).build()
        TestEntityUtils.setEntityId(testOrder, 1L)
    }

    @Test
    fun `✅주문 생성이 정상적으로 동작한다`() {
        // arrange
        val cmd = OrderCommand.Create.Root(
            userId = 1L,
            products = listOf(
                ProductInfo.CreateOrder.Root(
                    productId = 1L,
                    variants = listOf(ProductInfo.CreateOrder.Variant(
                        variantId = 1L,
                        unitPrice = Money.of(1000)
                    ))
                )),
            orderItems = listOf(
                OrderCommand.Create.OrderItem(
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1
                )
            ),
            timestamp = LocalDateTime.now()
        )
        whenever(orderRepository.save(any())).thenReturn(testOrder)

        // act
        val order = orderService.createOrder(cmd)

        // assert
        order.status shouldBe OrderStatus.CREATED
    }

    @Test
    fun `✅주문 완료가 정상적으로 동작한다`() {
        // arrange
        whenever(orderRepository.getById(any())).thenReturn(testOrder)
        whenever(orderRepository.save(any())).thenReturn(testOrder)

        val payload = PaymentCompletedPayload(
            orderId = 1L,
            userId = 1L,
            paymentId = 1L,
            pgPaymentId = "pg_12345",
            amount = Money.of(1000),
            timestamp = LocalDateTime.now()
        )

        // act
        shouldNotThrowAny { orderService.completeOrder(1L, payload) }

        // assert
        testOrder.status shouldBe OrderStatus.PAID
    }
} 