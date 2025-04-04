package kr.hhplus.be.server.product

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.common.PageInfo

@Schema(description = "상품 옵션 정보")
data class ProductVariantResponse(
    @Schema(description = "옵션 ID", example = "101")
    val variantId: Long,
    @Schema(description = "옵션명", example = "검정 / L")
    val option: String,
    @Schema(description = "가격", example = "20000")
    val price: Int,
    @Schema(description = "재고", example = "10")
    val stock: Int
)


@Schema(description = "상품 정보")
data class ProductResponse(

    @Schema(description = "상품 ID", example = "1")
    val productId: Long,

    @Schema(description = "상품 이름", example = "티셔츠")
    val name: String,

    @Schema(description = "상품 옵션 리스트")
    val variants: List<ProductVariantResponse>
)


@Schema(description = "상품 목록 응답")
data class ProductListResponse(

    @Schema(description = "상품 리스트")
    val products: List<ProductResponse>,

    @Schema(description = "페이지 정보")
    val pageInfo: PageInfo
)


@Schema(description = "인기 상품 정보")
data class PopularProductResponse(

    @Schema(description = "상품 ID", example = "1")
    val productId: Long,

    @Schema(description = "상품 이름", example = "반팔티")
    val name: String,

    @Schema(description = "총 판매 수량", example = "37")
    val totalSold: Int
)

