package kr.hhplus.be.server.product.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.product.domain.stats.*
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ProductAggregationService(
    private val productSalesLogRepository: ProductSalesLogRepository,
    private val productSalesAggregationDailyRepository: ProductSalesAggregationDailyRepository,
    private val productSalesAggregationDailyCheckpointRepository: ProductSalesAggregationDailyCheckpointRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository
) {
    @Transactional
    @CacheEvict(
        cacheNames = ["popularProduct"],
        key = "'cache:product:popular'"
    )
    fun aggregateSinceLastSummary(batchSize: Long, now: LocalDate) {
        val lastCheckpoint = productSalesAggregationDailyCheckpointRepository.findLast()
        val lastDailyLogId = lastCheckpoint?.lastAggregatedLogId ?: 0L
        val productSalesLogs = productSalesLogRepository.findForBatch(lastDailyLogId, batchSize)

        val logsGroupedByProduct = productSalesLogs.groupBy { it.productId }
        val productIds = productSalesLogs.map { ProductSalesAggregationDailyId(it.productId, now)}
        val dailyAggs = productSalesAggregationDailyRepository.findAll(productIds)

        logsGroupedByProduct.forEach { (productId, logList) ->
            val salesCount = calculateSalesCount(logList)
            upsertDailyAggregation(productId, now, dailyAggs, salesCount)
        }

        saveNewCheckpointIfAnyLogs(productSalesLogs)
        updateTopRankings(now)
    }

    private fun saveNewCheckpointIfAnyLogs(productSalesLogs: List<ProductSalesLog>) {
        if(productSalesLogs.isEmpty()) return

        val newLastDailyLogId = productSalesLogs.first().id
        val checkpoint = ProductSalesAggregationDailyCheckpoint(
            lastAggregatedLogId = newLastDailyLogId!!,
            lastAggregatedAt = LocalDateTime.now())
        productSalesAggregationDailyCheckpointRepository.save(checkpoint)
    }

    private fun upsertDailyAggregation(
        productId: Long,
        now: LocalDate,
        dailyAggs: List<ProductSalesAggregationDaily>,
        salesCount: Long
    ) {
        val aggId = ProductSalesAggregationDailyId(productId, now)
        val dailyAgg = dailyAggs.find { it.id == aggId }
        val updatedDailyAgg = dailyAgg
            ?.run { accumulate(salesCount) }
            ?: run { ProductSalesAggregationDaily(aggId, salesCount) }

        productSalesAggregationDailyRepository.save(updatedDailyAgg)
    }

    private fun calculateSalesCount(logList: List<ProductSalesLog>): Long {
        val soldQty = logList.filter { it.type == TransactionType.SOLD }.sumOf { it.quantity }
        val returnQty = logList.filter { it.type == TransactionType.RETURN }.sumOf { it.quantity }

        return soldQty - returnQty
    }

    private fun updateTopRankings(today: LocalDate, topN: Int = 5) {
        val from = today.minusDays(2)
        popularProductsDailyRepository.deleteAllById(
            (1..topN).map { PopularProductDailyId(today, it) }
        )

        val topProducts = productSalesAggregationDailyRepository
            .findTopProductsForRange(from, today, topN)

        val rankEntities = topProducts.mapIndexed { index, row ->
            PopularProductsDaily(
                id = PopularProductDailyId(today, index + 1),
                productId = (row["product_id"] as Number).toLong(),
                totalSales = (row["total_sales"] as Number).toLong()
            )
        }

        popularProductsDailyRepository.saveAll(rankEntities)
    }
}