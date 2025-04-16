package kr.hhplus.be.server.product.domain.stats

interface ProductSalesAggregationDailyRepository {
    fun save(entity: ProductSalesAggregationDaily): ProductSalesAggregationDaily
    fun getById(id: ProductSalesAggregationDailyId): ProductSalesAggregationDaily
}