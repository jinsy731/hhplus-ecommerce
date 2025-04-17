package kr.hhplus.be.server.product.domain.stats

interface ProductSalesAggregate {
    fun getProductId(): Long
    fun getTotalSales(): Long
}