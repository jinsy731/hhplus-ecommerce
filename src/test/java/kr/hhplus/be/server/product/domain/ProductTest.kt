package kr.hhplus.be.server.product.domain

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.ProductUnavailableException
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductVariant
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

class ProductTest {
    
    @ParameterizedTest
    @EnumSource(value = kr.hhplus.be.server.product.domain.product.ProductStatus::class, names = ["ON_SALE", "PARTIALLY_OUT_OF_STOCK"])
    fun `✅주문 시 상품 검증 성공`(status: kr.hhplus.be.server.product.domain.product.ProductStatus) {
        // arrange
        val product = ProductTestFixture.createValidProduct(id = 1L, variantIds = listOf(1,2)).apply { this.status = status }

        // act, assert
        shouldNotThrowAny { product.validatePurchasability(variantId = 1L, quantity = 1) }
    }

    @ParameterizedTest
    @EnumSource(value = kr.hhplus.be.server.product.domain.product.ProductStatus::class, names = ["DRAFT", "OUT_OF_STOCK", "HIDDEN", "DISCONTINUED"])
    fun `⛔️주문 시 상품 검증 실패`(status: kr.hhplus.be.server.product.domain.product.ProductStatus) {
        // arrange
        val product = kr.hhplus.be.server.product.domain.product.Product(
            name = "상품 A",
            basePrice = Money(BigDecimal(1000)),
            status = status
        )
        // act, assert
        shouldThrowExactly<ProductUnavailableException> { product.validatePurchasability(1L, 1) }
    }
    
    @Test
    fun `✅상품 옵션 가격 조회`() {
        // arrange
        val product = kr.hhplus.be.server.product.domain.product.Product(
            name = "상품 A",
            basePrice = Money(BigDecimal(1000)),
            status = kr.hhplus.be.server.product.domain.product.ProductStatus.ON_SALE,
            variants = mutableListOf(
                ProductVariant(
                    id = 1L,
                    additionalPrice = Money(BigDecimal(500)),
                    stock = 10
                )
            )
        )
        // act
        val variantPrice = product.getFinalPriceWithVariant(1L)
        // assert
        variantPrice shouldBe Money.of(1500)
    }
    
    @Test
    fun `✅상품 구매_검증이 통과하면 상품옵션별 재고가 감소해야한다`() {
        // arrange
        val product = ProductTestFixture.createValidProduct(variantIds = listOf(1,2))
        // act
        product.reduceStockByPurchase(1L, 1)
        // assert
        product.variants.find { it.id == 1L }!!.stock shouldBe 9
    }
}