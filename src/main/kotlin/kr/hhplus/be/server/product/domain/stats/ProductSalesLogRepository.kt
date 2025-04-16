package kr.hhplus.be.server.product.domain.stats

interface ProductSalesLogRepository {
    fun save(entity: ProductSalesLog): ProductSalesLog
    fun findForBatch(lastId: Long = 0L, limit: Long = 1000L): List<ProductSalesLog>
}