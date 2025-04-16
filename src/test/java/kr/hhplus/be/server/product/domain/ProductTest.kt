package kr.hhplus.be.server.product.domain

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.exception.ProductUnavailableException
import kr.hhplus.be.server.product.ProductTestFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

class ProductTest {
    
    @ParameterizedTest
    @EnumSource(value = ProductStatus::class, names = ["ON_SALE", "PARTIALLY_OUT_OF_STOCK"])
    fun `✅주문 시 상품 검증 성공`(status: ProductStatus) {
        // arrange
        val product = ProductTestFixture.createValidProduct(id = 1L).apply { this.status = status }

        // act, assert
        shouldNotThrowAny { product.validatePurchasability(variantId = 1L, quantity = 1) }
    }

    @ParameterizedTest
    @EnumSource(value = ProductStatus::class, names = ["DRAFT", "OUT_OF_STOCK", "HIDDEN", "DISCONTINUED"])
    fun `⛔️주문 시 상품 검증 실패`(status: ProductStatus) {
        // arrange
        val product = Product(
            name = "상품 A",
            basePrice = BigDecimal(1000),
            status = status
        )
        // act, assert
        shouldThrowExactly<ProductUnavailableException> { product.validatePurchasability(1L, 1) }
    }
    
    @Test
    fun `✅상품 옵션 가격 조회`() {
        // arrange
        val product = Product(
            name = "상품 A",
            basePrice = BigDecimal(1000),
            status = ProductStatus.ON_SALE,
            variants = mutableListOf(ProductVariant(
                id = 1L,
                additionalPrice = BigDecimal(500),
                stock = 10
            ))
        )
        // act
        val variantPrice = product.getVariantPrice(1L)
        // assert
        variantPrice shouldBe BigDecimal(1500)
    }
    
    @Test
    fun `✅상품 구매_검증이 통과하면 상품옵션별 재고가 감소해야한다`() {
        // arrange
        val product = ProductTestFixture.createValidProduct()
        // act
        product.reduceStockByPurchase(1L, 1)
        // assert
        product.variants.find { it.id == 1L }!!.stock shouldBe 9
    }
}