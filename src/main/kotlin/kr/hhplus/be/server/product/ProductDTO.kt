package kr.hhplus.be.server.product

import kr.hhplus.be.server.common.PageInfo

data class ProductVariantResponse(
    val variantId: Long,
    val option: String,
    val price: Int,
    val stock: Int
)

data class ProductResponse(
    val productId: Long,
    val name: String,
    val variants: List<ProductVariantResponse>
)

data class ProductListResponse(
    val products: List<ProductResponse>,
    val pageInfo: PageInfo
)

data class PopularProductResponse(
    val productId: Long,
    val name: String,
    val totalSold: Int
)
