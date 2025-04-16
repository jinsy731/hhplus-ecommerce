package kr.hhplus.be.server.product.domain.stats

interface ProductSalesAggregationCheckpointRepository {
    fun save(entity: ProductSalesAggregationCheckpoint): ProductSalesAggregationCheckpoint
    fun findLast(): ProductSalesAggregationCheckpoint?
}