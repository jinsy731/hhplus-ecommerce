package kr.hhplus.be.server.rank

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RankingService(private val productRankingRepository: ProductRankingRepository) {

    @Async
    fun updateProductRanking(cmd: RankingCommand.UpdateProductRanking.Root) {
        cmd.items.forEach { item ->
            productRankingRepository.increaseRanking(
                date = cmd.timestamp.toLocalDate(),
                productId = item.productId,
                quantity = item.quantity.toInt())
        }
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