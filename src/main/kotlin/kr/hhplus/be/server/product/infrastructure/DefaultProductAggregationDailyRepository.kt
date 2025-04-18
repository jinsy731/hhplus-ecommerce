package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregate
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@Repository
class DefaultProductAggregationDailyRepository(
    private val jpaRepository: JpaProductSalesAggregationDailyRepository,
): ProductSalesAggregationDailyRepository
{
    override fun save(entity: ProductSalesAggregationDaily): ProductSalesAggregationDaily {
        return jpaRepository.save(entity)
    }

    override fun findById(id: ProductSalesAggregationDailyId): ProductSalesAggregationDaily? {
        return jpaRepository.findById(id).getOrNull()
    }

    override fun findAll(ids: List<ProductSalesAggregationDailyId>): List<ProductSalesAggregationDaily> {
        return jpaRepository.findAllById(ids)
    }

}

interface JpaProductSalesAggregationDailyRepository: JpaRepository<ProductSalesAggregationDaily, ProductSalesAggregationDailyId> {
    @Query(
        value = """
            SELECT 
                product_id AS productId,
                SUM(sales_count) AS totalSales
            FROM p_sales_agg_day
            WHERE sales_day BETWEEN :fromDate AND :toDate
            GROUP BY product_id
            ORDER BY totalSales DESC
            LIMIT :limit
    """,
        nativeQuery = true
    )
    fun findPopularProducts(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate,
        @Param("limit") limit: Int
    ): List<ProductSalesAggregate>
}
