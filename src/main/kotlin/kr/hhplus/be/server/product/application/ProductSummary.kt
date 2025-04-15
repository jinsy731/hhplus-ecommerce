package kr.hhplus.be.server.product.application

import java.math.BigDecimal

class ProductSummary {
    data class Product(
        val id: Long,
        val name: String,
        val variants: List<ProductVariant>
    )
    data class ProductVariant(
        val id: Long,
        val variantValue: String,
        val unitPrice: BigDecimal
    )
}