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
    @Query("""
        SELECT new kr.hhplus.be.server.product.domain.stats.ProductSalesAggregate(
            p.id, p.name, SUM(psa.salesCount)
        )
        FROM Product p
        JOIN ProductSalesAggregationDaily psa ON p.id = psa.id.productId
        WHERE psa.id.salesDay BETWEEN :fromDate AND :toDate
        GROUP BY p.id, p.name
        ORDER BY SUM(psa.salesCount) DESC
        LIMIT :limit
    """)
    fun findPopularProducts(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate,
        @Param("limit") limit: Int
    ): List<ProductSalesAggregate>
}
