package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpoint
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpointRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultProductAggregationDailyCheckpointRepository(
    private val jpaRepository: JpaProductSalesAggregationDailyCheckpointRepository
): ProductSalesAggregationDailyCheckpointRepository {
    override fun save(entity: ProductSalesAggregationDailyCheckpoint): ProductSalesAggregationDailyCheckpoint {
        return jpaRepository.save(entity)
    }

    override fun findLast(): ProductSalesAggregationDailyCheckpoint? {
        return jpaRepository.findTopByOrderByIdDesc()
    }
}

interface JpaProductSalesAggregationDailyCheckpointRepository: JpaRepository<ProductSalesAggregationDailyCheckpoint, Long> {
    fun findTopByOrderByIdDesc(): ProductSalesAggregationDailyCheckpoint?
}