package kr.hhplus.be.server.payment.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.exception.AlreadyPaidException
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.toPreparePaymentContext
import kr.hhplus.be.server.payment.domain.Payment
import kr.hhplus.be.server.payment.domain.PaymentMethodType
import kr.hhplus.be.server.payment.domain.PaymentStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentTest {

    @Test
    fun `✅결제 생성`() {
        // arrange
        val now = LocalDateTime.now()

        val cmd = PaymentCommand.Prepare.Root(
            order = PaymentCommand.Prepare.OrderInfo(
                id = 1L,
                userId = 1L,
                items = listOf(
                    PaymentCommand.Prepare.OrderItemInfo(
                        id = 1L,
                        productId = 1L,
                        variantId = 1L,
                        subTotal = BigDecimal(1000),
                        discountedAmount = BigDecimal(500)
                    ),
                    PaymentCommand.Prepare.OrderItemInfo(
                        id = 2L,
                        productId = 1L,
                        variantId = 2L,
                        subTotal = BigDecimal(1000),
                        discountedAmount = BigDecimal(500)
                    ),
                ),
                originalTotal = BigDecimal(2000),
                discountedAmount = BigDecimal(1000)
            ),
            timestamp = now,
            payMethods = listOf(PaymentCommand.Prepare.PayMethod("POINT", BigDecimal(1000)))
        )
        val context = cmd.toPreparePaymentContext()
        // act
        val payment = Payment.create(context)
        
        // assert
        payment.orderId shouldBe 1L
        payment.originalAmount shouldBe BigDecimal(2000)
        payment.discountedAmount shouldBe BigDecimal(1000)
        payment.details shouldHaveSize 2
        payment.details[0].originalAmount shouldBe BigDecimal(1000)
        payment.details[0].discountedAmount shouldBe BigDecimal(500)
        payment.details[0].orderItemId shouldBe 1L
        payment.details[1].originalAmount shouldBe BigDecimal(1000)
        payment.details[1].discountedAmount shouldBe BigDecimal(500)
        payment.details[1].orderItemId shouldBe 2L
        payment.methods[0].type shouldBe PaymentMethodType.POINT
        payment.methods[0].amount shouldBe BigDecimal(1000)
    }
    
    @Test
    fun `✅결제 완료`() {
        // arrange
        val payment = Payment(
            orderId = 1L,
            originalAmount = BigDecimal(100),
            discountedAmount = BigDecimal(10),
            status = PaymentStatus.PENDING
        )
        // act
        payment.completePayment()
        
        // assert
        payment.status shouldBe PaymentStatus.PAID
    }


    @ParameterizedTest
    @EnumSource(value = PaymentStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING", "FAILED"])
    fun `⛔️결제 완료 실패`(status: PaymentStatus) {
        // arrange
        val payment = Payment(
            orderId = 1L,
            originalAmount = BigDecimal(100),
            discountedAmount = BigDecimal(10),
            status = status
        )
        // act, assert
        shouldThrowExactly<AlreadyPaidException> { payment.completePayment() }
    }
}