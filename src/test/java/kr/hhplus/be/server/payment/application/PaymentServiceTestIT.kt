package kr.hhplus.be.server.payment.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.common.exception.AlreadyPaidException
import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.payment.domain.PaymentMethodType
import kr.hhplus.be.server.payment.domain.PaymentRepository
import kr.hhplus.be.server.payment.domain.PaymentStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTestWithMySQLContainer
class PaymentServiceTestIT {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Test
    @Transactional
    fun `✅결제 준비를 하면 결제 정보가 저장된다`() {
        // given
        val cmd = createPreparePaymentCommand()

        // when
        val payment = paymentService.preparePayment(cmd)

        // then
        payment.id shouldBeGreaterThan 0L
        payment.orderId shouldBe cmd.order.id
        payment.originalAmount shouldBe cmd.order.originalTotal
        payment.discountedAmount shouldBe cmd.order.discountedAmount
        payment.status shouldBe PaymentStatus.PENDING
        payment.timestamp shouldBe cmd.timestamp
        
        payment.details shouldHaveSize 2
        payment.methods shouldHaveSize 2
        
        // DB에 저장된 결제 정보 확인
        val savedPayment = paymentRepository.getById(payment.id)
        savedPayment.id shouldBe payment.id
        savedPayment.status shouldBe PaymentStatus.PENDING
    }

    @Test
    @Transactional
    fun `✅결제 완료처리를 하면 결제 상태가 PAID로 변경된다`() {
        // given
        val prepareCmd = createPreparePaymentCommand()
        val payment = paymentService.preparePayment(prepareCmd)
        
        val completeCmd = PaymentCommand.Complete(paymentId = payment.id)
        
        // when
        paymentService.completePayment(completeCmd)
        
        // then
        val completedPayment = paymentRepository.getById(payment.id)
        completedPayment.status shouldBe PaymentStatus.PAID
    }

    @Test
    @Transactional
    fun `❌이미 완료된 결제를 다시 완료처리하면 예외가 발생한다`() {
        // given
        val prepareCmd = createPreparePaymentCommand()
        val payment = paymentService.preparePayment(prepareCmd)
        
        val completeCmd = PaymentCommand.Complete(paymentId = payment.id)
        paymentService.completePayment(completeCmd)
        
        // when & then
        shouldThrowExactly<AlreadyPaidException> {
            paymentService.completePayment(completeCmd)
        }
    }
    
    @Test
    @Transactional
    fun `❌존재하지 않는 결제 ID로 완료처리하면 예외가 발생한다`() {
        // given
        val nonExistentPaymentId = 999999L
        val completeCmd = PaymentCommand.Complete(paymentId = nonExistentPaymentId)
        
        // when & then
        shouldThrowExactly<ResourceNotFoundException> {
            paymentService.completePayment(completeCmd)
        }
    }

    private fun createPreparePaymentCommand(): PaymentCommand.Prepare.Root {
        val orderItemInfo1 = PaymentCommand.Prepare.OrderItemInfo(
            id = 101L,
            productId = 1001L,
            variantId = 10001L,
            subTotal = BigDecimal("50000"),
            discountedAmount = BigDecimal("5000")
        )
        
        val orderItemInfo2 = PaymentCommand.Prepare.OrderItemInfo(
            id = 102L,
            productId = 1002L,
            variantId = 10002L,
            subTotal = BigDecimal("30000"),
            discountedAmount = BigDecimal("3000")
        )
        
        val orderInfo = PaymentCommand.Prepare.OrderInfo(
            id = 1L,
            userId = 100L,
            items = listOf(orderItemInfo1, orderItemInfo2),
            originalTotal = BigDecimal("80000"),
            discountedAmount = BigDecimal("8000")
        )
        
        val payMethod1 = PaymentCommand.Prepare.PayMethod(
            type = PaymentMethodType.CREDIT_CARD.name,
            amount = BigDecimal("60000")
        )
        
        val payMethod2 = PaymentCommand.Prepare.PayMethod(
            type = PaymentMethodType.POINT.name,
            amount = BigDecimal("12000")
        )
        
        return PaymentCommand.Prepare.Root(
            order = orderInfo,
            timestamp = LocalDateTime.now(),
            payMethods = listOf(payMethod1, payMethod2)
        )
    }
}
