package kr.hhplus.be.server.product

import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.product.domain.ProductStatus
import kr.hhplus.be.server.product.domain.ProductVariant
import java.math.BigDecimal

object ProductTestFixture {
    fun createValidProduct(id: Long = 1L) = Product(
        id = id,
        name = "테스트 상품",
        basePrice = BigDecimal(1000),
        variants = createValidVariants(),
        status = ProductStatus.ON_SALE
    )

    fun createInvalidProduct(id: Long = 1L, status: ProductStatus = ProductStatus.OUT_OF_STOCK) = Product(
        id = id,
        name = "구매 불가 상품",
        basePrice = BigDecimal(1000),
        variants = createInvalidVariants(),
        status = status
    )

    private fun createValidVariants() = mutableListOf(
        ProductVariant(
            id = 1L,
            additionalPrice = BigDecimal(500),
            stock = 10,
        ),
        ProductVariant(
            id = 2L,
            additionalPrice = BigDecimal(100),
            stock = 5,
        ),
    )

    private fun createInvalidVariants() = mutableListOf(
        ProductVariant(
            id = 1L,
            additionalPrice = BigDecimal(500),
            stock = 0,
        ),
        ProductVariant(
            id = 2L,
            additionalPrice = BigDecimal(100),
            stock = 0,
        ),
    )
}