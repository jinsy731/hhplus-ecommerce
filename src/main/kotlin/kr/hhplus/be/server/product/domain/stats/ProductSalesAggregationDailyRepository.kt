package kr.hhplus.be.server.product.domain.stats

import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductSalesAggregationDailyRepository {
    fun save(entity: ProductSalesAggregationDaily): ProductSalesAggregationDaily
    fun findById(id: ProductSalesAggregationDailyId): ProductSalesAggregationDaily?
    fun findAll(ids: List<ProductSalesAggregationDailyId>): List<ProductSalesAggregationDaily>
    fun findTopProductsForRange(
        from: LocalDate,
        to: LocalDate,
        limit: Int
    ): List<Map<String, Any>>}