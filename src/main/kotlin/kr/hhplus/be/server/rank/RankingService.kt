package kr.hhplus.be.server.rank

import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RankingService(
    private val productRankingRepository: ProductRankingRepository,
    private val productRepository: ProductRepository
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
        cacheNames = ["cache"],
        key = "'product:ranking:daily'"
    )
    fun retrieveProductRanking(query: RankingQuery.RetrieveProductRanking): RankingResult.RetrieveProductRanking.Root {
        val topProductIds = productRankingRepository.getTopN(query.from, query.to, query.topN)
        val topProducts = productRepository.findAll(topProductIds)
        val productMap = topProducts.associateBy { it.id!! }
        val orderedProducts = topProductIds.map { productMap[it]!! }

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