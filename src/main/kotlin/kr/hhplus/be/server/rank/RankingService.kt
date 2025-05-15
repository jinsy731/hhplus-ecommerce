package kr.hhplus.be.server.rank

import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.shared.cache.CacheKey
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RankingService(
    private val productRankingRepository: ProductRankingRepository,
    private val productRepository: ProductRepository,
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
        key = "'product:daily'"
    )
    fun retrieveProductRanking(query: RankingQuery.RetrieveProductRanking): RankingResult.RetrieveProductRanking.Root {
        val topProductIds = productRankingRepository.getTopN(query.from, query.to, query.topN)
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
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY
        val cache = cacheManager.getCache(cacheName) ?: return

        cache.put(cacheKey, retrieveProductRanking(query))
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

class RankingQuery {
    data class RetrieveProductRanking(
        val from: LocalDate,
        val to: LocalDate,
        val topN: Long
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