package kr.hhplus.be.server.order.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.AlreadyPaidOrderException
import kr.hhplus.be.server.coupon.application.DiscountInfo
import kr.hhplus.be.server.order.domain.Order
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.OrderStatus
import kr.hhplus.be.server.product.application.ProductInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class OrderServiceTestIT {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Test
    @DisplayName("주문 생성 시 주문 정보가 정확하게 저장된다")
    @Transactional
    fun createOrder_shouldPersistCorrectly() {
        // given
        val userId = 1L
        val cmd = createOrderCommand(userId)

        // when
        val order = orderService.createOrder(cmd)

        // then
        order.id shouldBeGreaterThan 0L
        order.userId shouldBe userId
        order.status shouldBe OrderStatus.CREATED
        order.originalTotal shouldBe Money.of(10000)
        order.discountedAmount shouldBe Money.ZERO
        order.orderItems.size() shouldBe 2

        val savedOrder = orderRepository.getById(order.id)
        savedOrder.id shouldBe order.id
        savedOrder.status shouldBe OrderStatus.CREATED
    }

    @Test
    @DisplayName("주문에 할인 적용 시 최종 금액이 정확하게 계산된다")
    @Transactional
    fun applyDiscount_shouldUpdateFinalTotal() {
        // given
        val order = createAndSaveOrder()
        val orderItems = order.orderItems.asList()

        val discountInfos = listOf(
            DiscountInfo(orderItemId = orderItems[0].id, amount = Money.of(2000), sourceId = 1L, sourceType = "COUPON"),
            DiscountInfo(orderItemId = orderItems[1].id, amount = Money.of(3000), sourceId = 1L, sourceType = "COUPON")
        )
        val cmd = OrderCommand.ApplyDiscount(order.id, discountInfos)

        // when
        orderService.applyDiscount(cmd)

        // then
        val updatedOrder = orderRepository.getById(order.id)
        val updatedOrderItems = updatedOrder.orderItems.asList()
        updatedOrder.discountedAmount shouldBe Money.of(5000)
        updatedOrderItems[0].discountAmount shouldBe Money.of(2000)
        updatedOrderItems[1].discountAmount shouldBe Money.of(3000)
        updatedOrder.finalTotal() shouldBe updatedOrder.originalTotal - updatedOrder.discountedAmount
    }

    @Test
    @DisplayName("할인 총액이 주문 총액을 초과하면 최대 주문 총액까지만 할인된다")
    @Transactional
    fun discountOverTotal_shouldBeCapped() {
        // given
        val order = createAndSaveOrder()
        val orderItems = order.orderItems.asList()

        val discountInfos = listOf(
            DiscountInfo(orderItemId = orderItems[0].id, amount = Money.of(6000), sourceId = 1L, sourceType = "COUPON"),
            DiscountInfo(orderItemId = orderItems[1].id, amount = Money.of(7000), sourceId = 1L, sourceType = "COUPON")
        )
        val cmd = OrderCommand.ApplyDiscount(order.id, discountInfos)

        // when
        orderService.applyDiscount(cmd)

        // then
        val updatedOrder = orderRepository.getById(order.id)
        updatedOrder.discountedAmount shouldBe updatedOrder.originalTotal
        updatedOrder.finalTotal() shouldBe Money.ZERO
    }

    @Test
    @DisplayName("주문 결제 시 상태가 PAID로 변경된다")
    @Transactional
    fun completeOrder_shouldChangeStatusToPaid() {
        // given
        val order = createAndSaveOrder()

        // when
        orderService.completeOrder(order.id)

        // then
        val completedOrder = orderRepository.getById(order.id)
        completedOrder.status shouldBe OrderStatus.PAID
    }

    @Test
    @DisplayName("이미 결제된 주문에 대해 결제 시도 시 예외가 발생한다")
    @Transactional
    fun completingAlreadyPaidOrder_shouldThrowException() {
        // given
        val order = createAndSaveOrder()
        orderService.completeOrder(order.id)

        // when & then
        shouldThrowExactly<AlreadyPaidOrderException> {
            orderService.completeOrder(order.id)
        }
    }

    private fun createOrderCommand(userId: Long = 1L): OrderCommand.Create.Root {
        val products = listOf(
            ProductInfo.CreateOrder.Root(
                productId = 1L,
                variants = listOf(
                    ProductInfo.CreateOrder.Variant(
                        variantId = 1L,
                        unitPrice = Money.of(1000)
                    )
                )
            )
        )

        val orderItems = listOf(
            OrderCommand.Create.OrderItem(
                productId = 1L,
                variantId = 1L,
                quantity = 5
            ),
            OrderCommand.Create.OrderItem(
                productId = 1L,
                variantId = 1L,
                quantity = 5
            )
        )

        return OrderCommand.Create.Root(
            userId = userId,
            products = products,
            orderItems = orderItems,
            timestamp = LocalDateTime.now()
        )
    }

    private fun createAndSaveOrder(userId: Long = 1L): Order {
        val cmd = createOrderCommand(userId)
        val order = orderService.createOrder(cmd)
        return orderRepository.getById(order.id)
    }
}
