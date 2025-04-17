package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.common.PageResult
import kr.hhplus.be.server.product.domain.product.OptionSpec
import kr.hhplus.be.server.product.domain.product.OptionValue
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant
import java.math.BigDecimal

class ProductResult {
    data class RetrieveList(
        val products: List<ProductDetail>,
        val pageResult: PageResult
    )

    data class PopularProduct(
        val productId: Long,
        val name: String,
        val totalSales: Int
    )

    data class OptionValueDetail(
        val id: Long,
        val value: String
    )

    data class OptionSpecDetail(
        val id: Long,
        val name: String,
        val displayOrder: Int,
        val values: List<OptionValueDetail>
    )

    data class ProductVariantDetail(
        val variantId: Long,
        val optionValueIds: List<Long>,
        val additionalPrice: BigDecimal,
        val status: String,
        val stock: Int
    )

    data class ProductDetail(
        val productId: Long,
        val name: String,
        val basePrice: BigDecimal,
        val status: ProductStatus,
        val optionSpecs: List<OptionSpecDetail>,
        val variants: List<ProductVariantDetail>
    )
}

fun Product.toProductDetail(): ProductResult.ProductDetail {
    return ProductResult.ProductDetail(
        productId = this.id ?: throw IllegalStateException("Product ID is null"),
        name = this.name,
        basePrice = this.basePrice,
        status = this.status,
        optionSpecs = this.optionSpecs.sortedBy { it.displayOrder }.map { it.toOptionSpecDetail() },
        variants = this.variants.sortedBy { it.id }.map { it.toProductVariantDetail() }
    )
}

fun OptionSpec.toOptionSpecDetail(): ProductResult.OptionSpecDetail {
    return ProductResult.OptionSpecDetail(
        id = this.id ?: throw IllegalStateException("OptionSpec ID is null"),
        name = this.name,
        displayOrder = this.displayOrder,
        values = this.optionValues.sortedBy { it.id }.map { it.toOptionValueDetail() }
    )
}

fun OptionValue.toOptionValueDetail(): ProductResult.OptionValueDetail {
    return ProductResult.OptionValueDetail(
        id = this.id ?: throw IllegalStateException("OptionValue ID is null"),
        value = this.value
    )
}

fun ProductVariant.toProductVariantDetail(): ProductResult.ProductVariantDetail {
    return ProductResult.ProductVariantDetail(
        variantId = this.id ?: throw IllegalStateException("Variant ID is null"),
        optionValueIds = this.optionValues.sortedBy { it },
        additionalPrice = this.additionalPrice,
        status = this.status.name,
        stock = this.stock
    )
}





