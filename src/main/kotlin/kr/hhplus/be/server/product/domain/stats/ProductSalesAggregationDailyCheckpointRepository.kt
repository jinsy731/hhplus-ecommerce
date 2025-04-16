package kr.hhplus.be.server.product.domain.stats

interface ProductSalesAggregationDailyCheckpointRepository {
    fun save(entity: ProductSalesAggregationDailyCheckpoint): ProductSalesAggregationDailyCheckpoint
    fun findLast(): ProductSalesAggregationDailyCheckpoint?
}