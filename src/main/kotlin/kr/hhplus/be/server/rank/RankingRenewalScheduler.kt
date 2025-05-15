package kr.hhplus.be.server.rank

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@Profile("!test")
class RankingRenewalScheduler(private val rankingService: RankingService) {

    @Scheduled(fixedDelay = 300_000)
    fun renewProductRankingCache() {
        RankingPeriod.entries.forEach {
            rankingService.renewProductRankingCache(RankingQuery.RetrieveProductRanking(it)) }
    }
}