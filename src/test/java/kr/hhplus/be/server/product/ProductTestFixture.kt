package kr.hhplus.be.server.product

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant

object ProductTestFixture {
    fun createValidProduct(id: Long? = null, variantIds: List<Long?> = listOf(null, null)) = Product(
        id = id,
        name = "테스트 상품",
        basePrice = Money.of(1000),
        status = ProductStatus.ON_SALE
    ).apply {
        variantIds.forEach {
            this.addVariant(createValidVariant(id = it, additionalPrice = Money.of(500)))
            this.addVariant(createValidVariant(id = it, additionalPrice = Money.of(100)))
        }
    }

    fun createInvalidProduct(id: Long? = null, variantIds: List<Long?> = listOf(null, null), status: ProductStatus = ProductStatus.OUT_OF_STOCK) = Product(
        id = id,
        name = "구매 불가 상품",
        basePrice = Money.of(1000),
        status = status
    ).apply {
        variantIds.forEach {
            this.addVariant(createInvalidVariant(id = it, additionalPrice = Money.of(500)))
            this.addVariant(createInvalidVariant(id = it, additionalPrice = Money.of(100)))
        }
    }

    private fun createValidVariant(id: Long? = null, additionalPrice: Money = Money.of(500)) = ProductVariant(
        id = id,
        additionalPrice = additionalPrice,
        stock = 10,
    )

    private fun createInvalidVariant(id: Long? = null, additionalPrice: Money = Money.of(500)) = ProductVariant(
        id = id,
        additionalPrice = additionalPrice,
        stock = 0,
    )
}