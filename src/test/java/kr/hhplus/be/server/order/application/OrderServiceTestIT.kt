package kr.hhplus.be.server.order.application

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.coupon.application.dto.DiscountInfo
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.domain.event.PaymentCompletedPayload
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.product.application.dto.ProductInfo
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
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
        order.id!! shouldBeGreaterThan 0L
        order.userId shouldBe userId
        order.status shouldBe OrderStatus.CREATED
        order.originalTotal shouldBe Money.of(10000)
        order.discountedAmount shouldBe Money.ZERO
        order.orderItems.size() shouldBe 2

        val savedOrder = orderRepository.getById(order.id!!)
        savedOrder.id shouldBe order.id
        savedOrder.status shouldBe OrderStatus.CREATED
    }

    @Test
    @DisplayName("주문에 할인 적용 시 최종 금액이 정확하게 계산된다")
    @Transactional
    fun applyDiscount_shouldUpdateFinalTotal() {
        // given
        val order = createOrderSheet()
        val orderItems = order.orderItems.asList()

        val discountInfos = listOf(
            DiscountInfo(orderItemId = orderItems[0].id!!, amount = Money.of(2000), sourceId = 1L, sourceType = "COUPON"),
        )

        // when
        orderService.applyDiscountToOrder(order.id!!, discountInfos)

        // then
        val updatedOrder = orderRepository.getById(order.id!!)
        val updatedOrderItems = updatedOrder.orderItems.asList()
        updatedOrder.discountedAmount shouldBe Money.of(2000)
        updatedOrderItems[0].discountAmount shouldBe Money.of(2000)
        updatedOrder.finalTotal() shouldBe updatedOrder.originalTotal - updatedOrder.discountedAmount
    }

    @Test
    @DisplayName("주문 결제 시 상태가 PAID로 변경된다")
    @Transactional
    fun completeOrder_shouldChangeStatusToPaid() {
        // given
        val order = createOrderSheet()

        // when
        orderService.completeOrder(order.id!!, PaymentCompletedPayload(
            orderId = order.id!!,
            userId = order.userId,
            paymentId = 1L,
            pgPaymentId = "",
            amount = Money.ZERO,
            timestamp = LocalDateTime.now()
        ))

        // then
        val completedOrder = orderRepository.getById(order.id!!)
        completedOrder.status shouldBe OrderStatus.PAID
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

    private fun createOrderSheet(userId: Long = 1L): Order {
        val cmd = OrderCommand.CreateOrderSheet.Root(
            userId = userId,
            products = listOf(ProductInfo.CreateOrder.Root(
                productId = 1L,
                variants = listOf(ProductInfo.CreateOrder.Variant(
                    variantId = 1L,
                    unitPrice = Money.of(1000),
                ))
            )),
            orderItems = listOf(OrderCommand.CreateOrderSheet.OrderItem(
                productId = 1L,
                variantId = 1L,
                quantity = 5
            )),
            userCouponIds = listOf(),
            timestamp = LocalDateTime.now()
        )
        val order = orderService.createOrderSheet(cmd)
        return orderRepository.getById(order.id!!)
    }
}
