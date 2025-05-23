package kr.hhplus.be.server.order.domain

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderContext
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.AlreadyPaidOrderException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OrderTest {
    
    @Test
    fun `✅주문 생성 시 상태는 CREATED이다`() {
        // arrange
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

        // act
        val order = Order.create(context)

        // assert
        order.status shouldBe OrderStatus.CREATED
    }

    @Test
    fun `✅주문 완료 시 상태는 PAID이다`() {
        // arrange
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
        val order = Order.create(context)

        // act
        order.completeOrder()

        // assert
        order.status shouldBe OrderStatus.PAID
    }

    @Test
    fun `⛔️이미 결제된 주문을 다시 결제하려고 하면 예외가 발생한다`() {
        // arrange
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
        val order = Order.create(context)
        order.completeOrder()

        // act, assert
        shouldThrowExactly<AlreadyPaidOrderException> { order.completeOrder() }
    }
} 