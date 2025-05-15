package kr.hhplus.be.server.rank

import kr.hhplus.be.server.RedisCleaner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.Mockito.times

@SpringBootTest
class RankingRenewalSchedulerTest @Autowired constructor(
    private val rankingRenewalScheduler: RankingRenewalScheduler,
    @MockitoBean private val rankingService: RankingService,
    private val redisCleaner: RedisCleaner
){

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }
    @Test
    fun `renewProductRankingCache는 rankingService의 retrieveProductRanking을 올바른 인자와 함께 호출한다`() {
        // when
        rankingRenewalScheduler.renewProductRankingCache()

        // then
        val argumentCaptor = argumentCaptor<RankingQuery.RetrieveProductRanking>()
        verify(rankingService, times(RankingPeriod.entries.size)).renewProductRankingCache(argumentCaptor.capture())

        val capturedQueries = argumentCaptor.allValues
        assert(capturedQueries.size == RankingPeriod.entries.size)
        capturedQueries.forEach { query ->
            assert(query is RankingQuery.RetrieveProductRanking)
        }
    }
} 