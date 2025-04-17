package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
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

}
