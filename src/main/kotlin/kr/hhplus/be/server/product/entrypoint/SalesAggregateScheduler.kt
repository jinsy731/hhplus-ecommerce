package kr.hhplus.be.server.product.entrypoint

import kr.hhplus.be.server.product.application.ProductAggregationService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SalesAggregateScheduler(private val productAggregationScheduler: ProductAggregationService) {

    @Scheduled(fixedDelay = 300000) // 이전 실행 끝난 후 10분마다
    @Profile("!test")
    fun runBatch() {
        productAggregationScheduler.aggregateSinceLastSummary(10, LocalDate.now())
    }
}