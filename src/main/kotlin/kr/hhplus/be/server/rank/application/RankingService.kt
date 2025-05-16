package kr.hhplus.be.server.rank.application

import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.rank.infrastructure.persistence.ProductRankingRepository
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSetting
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSettingRepository
import kr.hhplus.be.server.shared.cache.CacheKey
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RankingService(
    private val productRankingRepository: ProductRankingRepository,
    private val productRepository: ProductRepository,
    private val rankingSettingRepository: RankingSettingRepository,
    private val cacheManager: CacheManager
    ) {

    @Async
    fun updateProductRanking(cmd: RankingCommand.UpdateProductRanking.Root) {
        cmd.items.forEach { item ->
            productRankingRepository.increaseRanking(
                date = cmd.timestamp.toLocalDate(),
                productId = item.productId,
                quantity = item.quantity.toInt())
        }
    }

    @Cacheable(
        cacheNames = [CacheKey.PRODUCT_RANKING_CACHE_NAME],
        key = "'product:' + #query.periodType"
    )
    fun retrieveProductRanking(query: RankingQuery.RetrieveProductRanking): RankingResult.RetrieveProductRanking.Root {
        val (from, to, topN) = resolveQueryProperties(query.periodType)

        val topProductIds = productRankingRepository.getTopN(from, to, topN)
        val topProducts = productRepository.findAll(topProductIds)
        val productMap = topProducts.associateBy { it.id!! }
        val orderedProducts = topProductIds.mapNotNull { productMap[it] }

        return RankingResult.RetrieveProductRanking.Root(
            products = orderedProducts.mapIndexed { index, product ->
                RankingResult.RetrieveProductRanking.ProductRanking(
                    rank = index + 1,
                    productId = product.id!!,
                    name = product.name
                )
            }
        )
    }

    fun renewProductRankingCache(query: RankingQuery.RetrieveProductRanking) {
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY_PREFIX + query.periodType.name
        val cache = cacheManager.getCache(cacheName) ?: return

        cache.put(cacheKey, retrieveProductRanking(query))
    }

    fun resolveQueryProperties(periodType: RankingPeriod): Triple<LocalDate, LocalDate, Long> {
        val DEFAULT_TOP_N = 5L
        val today = LocalDate.now()
        val from = today.minusDays(periodType.periodDays)
        val setting = rankingSettingRepository.get(periodType) ?: let {
            val defaultSetting = RankingSetting(DEFAULT_TOP_N)
            rankingSettingRepository.save(periodType, defaultSetting)
        }

        return Triple(from, today, setting.topN)
    }
}

class RankingCommand {
    class UpdateProductRanking {
        data class Root(
            val items: List<Item>,
            val timestamp: LocalDateTime
        )

        data class Item(
            val productId: Long,
            val quantity: Long
        )
    }
}

enum class RankingPeriod(val periodDays: Long) {
    DAILY(3),
    WEEKLY(7),
    MONTHLY(30)
}

class RankingQuery {
    data class RetrieveProductRanking(
        val periodType: RankingPeriod
    )
}

class RankingResult {
    class RetrieveProductRanking {
        data class Root(
            val products: List<ProductRanking>
        )

        data class ProductRanking(
            val rank: Int,
            val productId: Long,
            val name: String
        )
    }
}