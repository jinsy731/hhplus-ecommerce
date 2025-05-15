package kr.hhplus.be.server.rank.entrypoint.scheduler

import kr.hhplus.be.server.rank.application.RankingPeriod
import kr.hhplus.be.server.rank.application.RankingQuery
import kr.hhplus.be.server.rank.application.RankingService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class RankingRenewalScheduler(private val rankingService: RankingService) {

    @Scheduled(fixedDelay = 300_000)
    fun renewProductRankingCache() {
        RankingPeriod.entries.forEach {
            rankingService.renewProductRankingCache(RankingQuery.RetrieveProductRanking(it)) }
    }
}