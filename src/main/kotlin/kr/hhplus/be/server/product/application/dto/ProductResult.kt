package kr.hhplus.be.server.product.application.dto

import kr.hhplus.be.server.product.domain.product.model.OptionSpec
import kr.hhplus.be.server.product.domain.product.model.OptionValue
import kr.hhplus.be.server.product.domain.product.model.ProductStatus
import kr.hhplus.be.server.product.domain.product.model.ProductVariant
import kr.hhplus.be.server.shared.domain.Money

class ProductResult {
    data class RetrieveList(
        val products: List<ProductSummary>,
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
        val additionalPrice: Money,
        val status: String,
        val stock: Int
    )

    data class ProductSummary(
        val productId: Long,
        val name: String,
        val basePrice: Money,
        val status: ProductStatus,
    )
}

// 매핑 함수는 ProductMapper로 이동됨

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





