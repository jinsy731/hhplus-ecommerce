package kr.hhplus.be.server.order.application

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderContext
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.shared.domain.DomainEvent
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class OrderServiceTest {
    private lateinit var orderService: OrderService
    private lateinit var orderRepository: OrderRepository
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var testOrder: Order

    @BeforeEach
    fun setup() {
        orderRepository = mock()
        eventPublisher = mock()
        orderService = OrderService(orderRepository, eventPublisher)

        val context = OrderContext.Create.Root(
            userId = 1L,
            timestamp = LocalDateTime.now(),
            items = listOf(
                OrderContext.Create.Item(
                    productId = 1L,
                    variantId = 1L,
                    quantity = 1,
                    unitPrice = Money.of(1000)
                )
            )
        )
        testOrder = Order.create(context)
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
        verify(eventPublisher).publish(any<DomainEvent<*>>())
        order.status shouldBe OrderStatus.CREATED
    }

    @Test
    fun `✅주문 완료가 정상적으로 동작한다`() {
        // arrange
        whenever(orderRepository.getById(any())).thenReturn(testOrder)
        whenever(orderRepository.save(any())).thenReturn(testOrder)

        // act
        shouldNotThrowAny { orderService.completeOrder(1L, OrderSagaContext(
            order = mockk<Order>(),
            timestamp = LocalDateTime.now()
        )) }

        // assert
        testOrder.status shouldBe OrderStatus.PAID
    }
} 