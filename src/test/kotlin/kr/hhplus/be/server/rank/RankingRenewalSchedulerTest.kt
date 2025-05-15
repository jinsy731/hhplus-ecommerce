package kr.hhplus.be.server.rank

import kr.hhplus.be.server.RedisCleaner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate

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
        verify(rankingService).renewProductRankingCache(argumentCaptor.capture())

        val capturedQuery = argumentCaptor.firstValue
        assert(capturedQuery.from == LocalDate.now().minusDays(2))
        assert(capturedQuery.to == LocalDate.now())
        assert(capturedQuery.topN == 5L)
    }
} 