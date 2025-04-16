package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpoint
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpointRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesLog
import kr.hhplus.be.server.product.domain.stats.ProductSalesLogRepository
import kr.hhplus.be.server.product.domain.stats.TransactionType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ProductAggregationService(
    private val productSalesLogRepository: ProductSalesLogRepository,
    private val productSalesAggregationDailyRepository: ProductSalesAggregationDailyRepository,
    private val productSalesAggregationDailyCheckpointRepository: ProductSalesAggregationDailyCheckpointRepository
) {
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
    }

    private fun saveNewCheckpointIfAnyLogs(productSalesLogs: List<ProductSalesLog>) {
        if(productSalesLogs.isEmpty()) return

        val newLastDailyLogId = productSalesLogs.first().id
        val checkpoint = ProductSalesAggregationDailyCheckpoint(newLastDailyLogId, LocalDateTime.now())
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
}