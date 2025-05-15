package kr.hhplus.be.server.rank

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingRenewalScheduler(private val rankingService: RankingService) {

    @Scheduled(fixedDelay = 300_000)
    fun renewProductRankingCache() {
        val query = RankingQuery.RetrieveProductRanking(
            from = LocalDate.now().minusDays(2),
            to = LocalDate.now(),
            topN = 5
        )
        rankingService.renewProductRankingCache(query)
    }
}