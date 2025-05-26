package kr.hhplus.be.server.rank.entrypoint.event

import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.rank.application.RankingCommand
import kr.hhplus.be.server.rank.application.RankingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SpringRankingOrderEventListener(private val rankingService: RankingService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCompleted(event: OrderEvent.Completed) {
        logger.info("[{}] Received OrderEvent.Completed: orderId={}, items={}",
            Thread.currentThread().name, event.payload.order.id, event.payload.order.orderItems)
        rankingService.updateProductRanking(
            RankingCommand.UpdateProductRanking.Root(
            items = event.payload.order.orderItems.map { RankingCommand.UpdateProductRanking.Item(it.productId, it.quantity.toLong()) },
            timestamp = event.payload.order.createdAt
        ))
    }
}