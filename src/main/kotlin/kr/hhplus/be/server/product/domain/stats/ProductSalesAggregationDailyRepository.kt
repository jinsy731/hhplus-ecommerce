package kr.hhplus.be.server.product.domain.stats

interface ProductSalesAggregationDailyRepository {
    fun save(entity: ProductSalesAggregationDaily): ProductSalesAggregationDaily
    fun findById(id: ProductSalesAggregationDailyId): ProductSalesAggregationDaily?
    fun findAll(ids: List<ProductSalesAggregationDailyId>): List<ProductSalesAggregationDaily>
}