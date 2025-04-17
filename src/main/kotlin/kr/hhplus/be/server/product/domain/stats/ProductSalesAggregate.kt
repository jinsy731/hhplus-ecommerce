package kr.hhplus.be.server.product.domain.stats

data class ProductSalesAggregate(
    val productId: Long,
    val productName: String,
    val totalSold: Long
)