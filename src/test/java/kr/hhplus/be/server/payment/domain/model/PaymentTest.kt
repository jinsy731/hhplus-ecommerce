package kr.hhplus.be.server.payment.domain.model

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.payment.application.PaymentCommand
import kr.hhplus.be.server.payment.application.toPreparePaymentContext
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.AlreadyPaidException
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
            timestamp = now
        )
        val context = cmd.toPreparePaymentContext()
        // act
        val payment = Payment.create(context)
        
        // assert
        payment.orderId shouldBe 1L
        payment.originalAmount shouldBe Money.of(2000)
        payment.discountedAmount shouldBe Money.of(1000)
        payment.status shouldBe PaymentStatus.PENDING
        payment.details shouldHaveSize 2
        payment.timestamp shouldBe now
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus::class)
    fun `✅결제 상태 변경`(status: PaymentStatus) {
        // arrange
        val payment = Payment(
            id = 1L,
            orderId = 1L,
            originalAmount = Money.of(1000),
            discountedAmount = Money.of(100),
            status = PaymentStatus.PENDING,
            timestamp = LocalDateTime.now()
        )

        // act & assert
        when (status) {
            PaymentStatus.PAID -> {
                payment.complete()
                payment.status shouldBe PaymentStatus.PAID
            }
            PaymentStatus.REFUNDED -> {
                payment.complete()
                payment.cancel()
                payment.status shouldBe PaymentStatus.REFUNDED
            }
            else -> {
                // 다른 상태들은 직접 테스트하지 않음
            }
        }
    }

    @Test
    fun `❌이미 완료된 결제를 다시 완료하면 예외가 발생한다`() {
        // arrange
        val payment = Payment(
            id = 1L,
            orderId = 1L,
            originalAmount = Money.of(1000),
            discountedAmount = Money.of(100),
            status = PaymentStatus.PAID,
            timestamp = LocalDateTime.now()
        )

        // act & assert
        shouldThrowExactly<AlreadyPaidException> {
            payment.complete()
        }
    }
}