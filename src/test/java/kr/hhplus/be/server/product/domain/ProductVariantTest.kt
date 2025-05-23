package kr.hhplus.be.server.product.domain

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.product.domain.product.model.ProductVariant
import kr.hhplus.be.server.product.domain.product.model.VariantStatus
import kr.hhplus.be.server.shared.exception.VariantOutOfStockException
import kr.hhplus.be.server.shared.exception.VariantUnavailableException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ProductVariantTest {
    
    @Test
    fun `✅상품옵션 구매 가능 검증 성공_상태가 ACTIVE이고 재고가 충분하면 예외를 발생시키지 않는다`() {
        // arrange
        val variant = ProductVariant(stock = 10, status = VariantStatus.ACTIVE)
        // act, assert
        shouldNotThrowAny { variant.checkAvailableToOrder(10) }
    }

    @ParameterizedTest
    @EnumSource(value = VariantStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["ACTIVE"])
    fun `⛔️상품옵션 구매 가능 검증 실패_상태가 ACTIVE가 아닌 경우 VariantUnavailableException 예외를 발생시켜야 한다`(status: VariantStatus) {
        // arrange
        val variant = ProductVariant(stock = 10, status = status)
        // act, assert
        shouldThrowExactly<VariantUnavailableException> { variant.checkAvailableToOrder(10) }
    }
    
    @Test
    fun `⛔️상품옵션 구매 가능 검증 실패_재고가 부족하면 VariantOutOfStockException 예외를 발생시켜야 한다`() {
        // arrange
        val variant = ProductVariant(stock = 10, status = VariantStatus.ACTIVE)
        // act, assert
        shouldThrowExactly<VariantOutOfStockException> { variant.checkAvailableToOrder(12) }
    }

    @Test
    fun `✅상품 재고 차감`() {
        // arrange
        val variant = ProductVariant(stock = 10, status = VariantStatus.ACTIVE)
        // act
        variant.reduceStock(5)
        // assert
        variant.stock shouldBe 5
    }

    @Test
    fun `⛔️상품 재고 차감 실패_재고가 부족한 경우 VariantOutOfStockException 예외를 발생시켜야 한다`() {
        // arrange
        val variant = ProductVariant(stock = 10, status = VariantStatus.ACTIVE)
        // act, assert
        shouldThrowExactly<VariantOutOfStockException> { variant.reduceStock(15) }
    }

    @Test
    fun `✅상품 재고 복구`() {
        // arrange
        val variant = ProductVariant(stock = 10, status = VariantStatus.ACTIVE)
        // act
        variant.restoreStock(5)
        // assert
        variant.stock shouldBe 15
    }
}