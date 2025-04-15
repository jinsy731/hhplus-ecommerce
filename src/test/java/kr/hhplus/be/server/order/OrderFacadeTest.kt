package kr.hhplus.be.server.order

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kr.hhplus.be.server.coupon.application.CouponResult
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import kr.hhplus.be.server.common.MessagingService
import kr.hhplus.be.server.order.application.OrderService
import kr.hhplus.be.server.order.facade.OrderCriteria
import kr.hhplus.be.server.order.facade.OrderFacade
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.PaymentService
import kr.hhplus.be.server.payment.domain.Payment
import kr.hhplus.be.server.payment.domain.PaymentMethodType
import kr.hhplus.be.server.product.application.ProductService
import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.user.application.UserPointService
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderFacadeTest {

    private val orderService = mockk<OrderService>(relaxed = true)
    private val couponService = mockk<CouponService>(relaxed = true)
    private val userPointService = mockk<UserPointService>(relaxed = true)
    private val paymentService = mockk<PaymentService>(relaxed = true)
    private val productService = mockk<ProductService>(relaxed = true)
    private val messageService = mockk<MessagingService>(relaxed = true)

    private val orderFacade = OrderFacade(
        orderService, couponService, userPointService, paymentService, productService, messageService
    )

    @Test
    fun `✅ 주문 흐름이 정상적으로 호출되는지 검증`() {
        // given
        val cri = Fixture.orderCriteria()
        val products = Fixture.products()
        val order = Fixture.order()
        val applyCouponResult = Fixture.applyCouponResult()
        val payment = Fixture.payment()

        every { productService.findAllById(any()) } returns products
        every { orderService.createOrder(any()) } returns order
        every { couponService.use(any()) } returns applyCouponResult
        every { paymentService.preparePayment(any()) } returns payment
        every { paymentService.completePayment(any()) } just Runs
        every { userPointService.use(any()) } just Runs
        every { orderService.completeOrder(any()) } just Runs
        every { messageService.publish(any()) } just Runs

        // when
        orderFacade.placeOrder(cri)

        // then
        verifyOrder {
            productService.findAllById(any())
            orderService.createOrder(any())
            couponService.use(any())
            paymentService.preparePayment(any())
            paymentService.completePayment(any())
            orderService.completeOrder(any())
            userPointService.use(any())
            messageService.publish(any())
        }
    }

    @Test
    fun `❌ 쿠폰 적용 중 예외 발생 시 이후 로직 호출되지 않아야 한다`() {
        // given
        val cri = Fixture.orderCriteria()
        val products = Fixture.products()
        val order = Fixture.order()

        every { productService.findAllById(any()) } returns products
        every { orderService.createOrder(any()) } returns order
        every { couponService.use(any()) } throws RuntimeException("쿠폰 오류")

        // when
        shouldThrow<RuntimeException> {
            orderFacade.placeOrder(cri)
        }

        // then
        verify(exactly = 1) { productService.findAllById(any()) }
        verify(exactly = 1) { orderService.createOrder(any()) }
        verify(exactly = 1) { couponService.use(any()) }

        verify(exactly = 0) { paymentService.preparePayment(any()) }
        verify(exactly = 0) { userPointService.use(any()) }
    }

    @Test
    fun `✅ 포인트 사용 금액은 주문 최종 금액과 같아야 한다`() {
        val cri = Fixture.orderCriteria()
        val order = Fixture.order(finalTotal = BigDecimal(5000))
        val products = Fixture.products()
        val payment = Fixture.payment()
        val applyCouponResult = Fixture.applyCouponResult()

        every { productService.findAllById(any()) } returns products
        every { orderService.createOrder(any()) } returns order
        every { couponService.use(any()) } returns applyCouponResult
        every { paymentService.preparePayment(any()) } returns payment
        every { paymentService.completePayment(any()) } just Runs
        every { userPointService.use(any()) } just Runs
        every { messageService.publish(any()) } just Runs
        every { orderService.completeOrder(any()) } just Runs

        orderFacade.placeOrder(cri)

        verify {
            userPointService.use(match {
                it.amount == order.finalTotal()
            })
        }
    }

}

object Fixture {
    fun orderCriteria() = OrderCriteria.PlaceOrder(
        userId = 1L,
        orderItem = listOf(OrderCriteria.OrderItem(1L, 2, 1)),
        userCouponIds = listOf(1,2,3),
        payMethods = listOf(PaymentCommand.PayMethod(PaymentMethodType.POINT, BigDecimal(100))),
        now = LocalDateTime.now()
    )

    fun products() = listOf(Product(1L, "티셔츠", BigDecimal(10000)))
    fun order(finalTotal: BigDecimal = BigDecimal(10000)) = OrderTestFixture.createOrder(1L)
    fun applyCouponResult() = CouponResult.Use(listOf(DiscountLine(1L,1L, DiscountMethod.COUPON, 1L,
        BigDecimal(100), LocalDateTime.now())))
    fun payment() = Payment(
        orderId = 1L,
        originalAmount = BigDecimal(100),
        discountedAmount = BigDecimal(50)
    )
}

