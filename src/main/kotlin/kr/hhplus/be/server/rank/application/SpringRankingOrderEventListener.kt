package kr.hhplus.be.server.rank.application

import kr.hhplus.be.server.order.domain.OrderEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringRankingOrderEventListener(private val rankingService: RankingService) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCompleted(event: OrderEvent.Completed) {
        rankingService.updateProductRanking(RankingCommand.UpdateProductRanking.Root(
            items = event.payload.order.orderItems.map { RankingCommand.UpdateProductRanking.Item(it.productId, it.quantity.toLong()) },
            timestamp = event.payload.order.createdAt
        ))
    }
}