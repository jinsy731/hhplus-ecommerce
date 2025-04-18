package kr.hhplus.be.server.product.entrypoint

import jakarta.transaction.Transactional
import kr.hhplus.be.server.product.application.ProductAggregationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class SalesAggregateScheduler(private val productAggregationScheduler: ProductAggregationService) {

    @Scheduled(fixedDelay = 300000) // 이전 실행 끝난 후 10분마다
    fun runBatch() {
        productAggregationScheduler.aggregateSinceLastSummary(10, LocalDate.now())
    }
}