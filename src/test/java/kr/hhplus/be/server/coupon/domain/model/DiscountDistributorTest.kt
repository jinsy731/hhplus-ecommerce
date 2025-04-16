package kr.hhplus.be.server.coupon.domain.model

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.domain.OrderItem
import kr.hhplus.be.server.order.domain.OrderItemStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class DiscountDistributorTest {

    @Test
    fun `✅ 쿠폰적용_distributeDiscount 메소드는 할인액을 정확히 분배한다`() {
        // arrange
        val item1 = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(6000),
            status = OrderItemStatus.ORDERED
        )

        val item2 = OrderItem(
            id = 2L,
            productId = 2L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(4000),
            status = OrderItemStatus.ORDERED
        )

        val totalDiscount = BigDecimal(1000)

        // act
        val result = DiscountDistributor.distribute(listOf(item1, item2), 1L, LocalDateTime.now(), totalDiscount)

        // assert
        result.size shouldBe 2

        // 금액 비율대로 분배 (6:4)
        val discount1 = result.find { it.orderItemId == 1L }!!.amount
        val discount2 = result.find { it.orderItemId == 2L }!!.amount

        discount1.compareTo(BigDecimal(600)) shouldBe 0
        discount2.compareTo(BigDecimal(400)) shouldBe 0

        // 할인액 합계가 총 할인액과 일치
        result.sumOf { it.amount }.compareTo(totalDiscount) shouldBe 0
    }

    @Test
    fun `✅ 쿠폰적용_마지막 아이템은 남은 할인 잔액을 모두 받는다`() {
        // arrange
        val item1 = OrderItem(
            id = 1L,
            productId = 1L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(3000),
            status = OrderItemStatus.ORDERED
        )

        val item2 = OrderItem(
            id = 2L,
            productId = 2L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(3000),
            status = OrderItemStatus.ORDERED
        )

        val item3 = OrderItem(
            id = 3L,
            productId = 3L,
            variantId = 1L,
            quantity = 1,
            unitPrice = BigDecimal(4000),
            status = OrderItemStatus.ORDERED
        )

        // 10000원 중 1000원 할인
        val totalDiscount = BigDecimal(1000)

        // act
        val result = DiscountDistributor.distribute(listOf(item1, item2, item3), 1L, LocalDateTime.now(), totalDiscount)

        // assert
        result.size shouldBe 3

        // 할인액 합계가 총 할인액과 일치하는지 확인
        result.sumOf { it.amount }.compareTo(totalDiscount) shouldBe 0

        // 마지막 아이템은 남은 할인 잔액을 모두 받아야 함
        // (반올림 오차가 마지막 아이템에 모두 반영됨)
        val discount3 = result.find { it.orderItemId == 3L }!!.amount
        val expected3 = BigDecimal(400)
        discount3.compareTo(expected3) shouldBe 0
    }
}