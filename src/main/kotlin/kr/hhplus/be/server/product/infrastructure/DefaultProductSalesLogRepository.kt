package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.stats.ProductSalesLog
import kr.hhplus.be.server.product.domain.stats.ProductSalesLogRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
class DefaultProductSalesLogRepository(private val jpaRepository: JpaProductSalesLogRepository): ProductSalesLogRepository {
    override fun save(entity: ProductSalesLog): ProductSalesLog {
        return jpaRepository.save(entity)
    }

    override fun findForBatch(
        lastId: Long,
        limit: Long
    ): List<ProductSalesLog> {
        return jpaRepository.findAfterLastIdWithLimit(lastId, limit)
    }
}

interface JpaProductSalesLogRepository: JpaRepository<ProductSalesLog, Long> {
    @Query(
        value = """
        SELECT * FROM product_sales_log
        WHERE product_sales_log_id > :lastId
        ORDER BY product_sales_log_id DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findAfterLastIdWithLimit(lastId: Long, limit: Long): List<ProductSalesLog>
}