package kr.hhplus.be.server.payment.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.AlreadyPaidException
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.toPreparePaymentContext
import kr.hhplus.be.server.payment.domain.Payment
import kr.hhplus.be.server.payment.domain.PaymentStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
                        subTotal = Money.of(1000),
                        discountedAmount = Money.of(500)
                    ),
                    PaymentCommand.Prepare.OrderItemInfo(
                        id = 2L,
                        productId = 1L,
                        variantId = 2L,
                        subTotal = Money.of(1000),
                        discountedAmount = Money.of(500)
                    ),
                ),
                originalTotal = Money.of(2000),
                discountedAmount = Money.of(1000)
            ),
            timestamp = now,
        )
        val context = cmd.toPreparePaymentContext()
        // act
        val payment = Payment.create(context)
        
        // assert
        payment.orderId shouldBe 1L
        payment.originalAmount shouldBe Money.of(2000)
        payment.discountedAmount shouldBe Money.of(1000)
        payment.details shouldHaveSize 2
        payment.details[0].originalAmount shouldBe Money.of(1000)
        payment.details[0].discountedAmount shouldBe Money.of(500)
        payment.details[0].orderItemId shouldBe 1L
        payment.details[1].originalAmount shouldBe Money.of(1000)
        payment.details[1].discountedAmount shouldBe Money.of(500)
        payment.details[1].orderItemId shouldBe 2L
    }
    
    @Test
    fun `✅결제 완료`() {
        // arrange
        val payment = Payment(
            orderId = 1L,
            originalAmount = Money.of(100),
            discountedAmount = Money.of(10),
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
            originalAmount = Money.of(100),
            discountedAmount = Money.of(10),
            status = status
        )
        // act, assert
        shouldThrowExactly<AlreadyPaidException> { payment.completePayment() }
    }
}