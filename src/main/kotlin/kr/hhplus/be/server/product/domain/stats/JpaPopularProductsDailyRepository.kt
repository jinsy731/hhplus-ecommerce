package kr.hhplus.be.server.product.domain.stats

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface JpaPopularProductsDailyRepository : JpaRepository<PopularProductsDaily, PopularProductDailyId> {
    @Query("""
    SELECT product_id, sales_count
    FROM p_sales_agg_day
    WHERE sales_day = :salesDay
    ORDER BY sales_count DESC
    LIMIT :limit
""", nativeQuery = true)
    fun findTopProductsByDay(
        @Param("salesDay") salesDay: LocalDate,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
}
