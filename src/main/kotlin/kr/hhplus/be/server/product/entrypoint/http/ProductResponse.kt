package kr.hhplus.be.server.product.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.common.PageInfo
import kr.hhplus.be.server.common.toResponse
import kr.hhplus.be.server.product.application.ProductResult
import kr.hhplus.be.server.product.domain.product.ProductStatus
import java.math.BigDecimal
import kotlin.collections.map

class ProductResponse {

    class Retrieve {
        @Schema(description = "상품 목록 응답")
        data class Lists(
            @Schema(description = "상품 리스트")
            val products: List<ProductDetail>,

            @Schema(description = "페이지 정보")
            val pageInfo: PageInfo
        )

        @Schema(description = "인기 상품 정보")
        data class Popular(
            @Schema(description = "상품 ID", example = "1")
            val productId: Long,

            @Schema(description = "상품 이름", example = "반팔티")
            val name: String,

            @Schema(description = "총 판매 수량", example = "37")
            val totalSold: Int
        )
    }


    @Schema(description = "옵션 스펙 값")
    data class OptionValueDetail(
        @Schema(description = "옵션 값 ID", example = "11")
        val id: Long,

        @Schema(description = "옵션 값", example = "검정")
        val value: String
    )

    @Schema(description = "옵션 스펙")
    data class OptionSpecDetail(
        @Schema(description = "옵션 스펙 ID", example = "1")
        val id: Long,

        @Schema(description = "옵션 스펙 이름", example = "색상")
        val name: String,

        @Schema(description = "옵션 스펙 노출 순서", example = "1")
        val displayOrder: Int,

        @Schema(description = "옵션 값 배열")
        val values: List<OptionValueDetail>
    )

    @Schema(description = "상품 옵션 조합")
    data class ProductVariantDetail(
        @Schema(description = "옵션 조합 ID", example = "101")
        val variantId: Long,

        @Schema(description = "옵션 조합에 포함된 옵션 스펙 IDs", example = "[1, 2]")
        val optionValueIds: List<Long>,

        @Schema(description = "옵션 조합에 따른 추가 가격", example = "1000")
        val additionalPrice: BigDecimal,

        @Schema(description = "옵션 조합 상태", example = "ACTIVE")
        val status: String,

        @Schema(description = "해당 옵션 조합의 재고", example = "10")
        val stock: Int
    )

    @Schema(description = "상품 정보")
    data class ProductDetail(
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,

        @Schema(description = "상품 이름", example = "티셔츠")
        val name: String,

        @Schema(description = "기본 금액", example = "29000")
        val basePrice: BigDecimal,

        @Schema(description = "상품 상태", example = "ON_SALE")
        val status: ProductStatus,

        @Schema(description = "옵션 스펙 배열")
        val optionSpecs: List<OptionSpecDetail>,

        @Schema(description = "옵션 조합 배열")
        val variants: List<ProductVariantDetail>
    )
}

fun ProductResult.RetrieveList.toProductResponse() = ProductResponse.Retrieve.Lists(
    products = this.products.toProductResponse(),
    pageInfo = this.pageResult.toResponse(),
)

fun List<ProductResult.ProductDetail>.toProductResponse() = this.map { ProductResponse.ProductDetail(
    productId = it.productId!!,
    name = it.name,
    basePrice = it.basePrice,
    status = it.status,
    optionSpecs = it.optionSpecs.toOptionSpecResponse(),
    variants = it.variants.toVaraintResponse()
)}.toList()

fun List<ProductResult.OptionValueDetail>.toOptionValueResponse() = this.map { ProductResponse.OptionValueDetail(
    id = it.id,
    value = it.value
)}

fun List<ProductResult.OptionSpecDetail>.toOptionSpecResponse() = this.map { ProductResponse.OptionSpecDetail(
    id = it.id,
    name = it.name,
    displayOrder = it.displayOrder,
    values = it.values.toOptionValueResponse()
)}

fun List<ProductResult.ProductVariantDetail>.toVaraintResponse() = this.map { ProductResponse.ProductVariantDetail(
    variantId = it.variantId!!,
    optionValueIds = it.optionValueIds,
    additionalPrice = it.additionalPrice,
    status = it.status,
    stock = it.stock
)}

/**
 * ProductResult.PopularProduct를 ProductResponse.Retrieve.Popular로 변환합니다.
 */
fun ProductResult.PopularProduct.toPopularProductResponse() = ProductResponse.Retrieve.Popular(
    productId = this.productId,
    name = this.name,
    totalSold = this.totalSales
)