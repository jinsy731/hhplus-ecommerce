package kr.hhplus.be.server.rank

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate

@SpringBootTest
class RankingRenewalSchedulerTest {

    @Autowired
    private lateinit var rankingRenewalScheduler: RankingRenewalScheduler

    @MockitoBean
    private lateinit var rankingService: RankingService

    @Test
    fun `renewProductRankingCache는 rankingService의 retrieveProductRanking을 올바른 인자와 함께 호출한다`() {
        // when
        rankingRenewalScheduler.renewProductRankingCache()

        // then
        val argumentCaptor = argumentCaptor<RankingQuery.RetrieveProductRanking>()
        verify(rankingService).retrieveProductRanking(argumentCaptor.capture())

        val capturedQuery = argumentCaptor.firstValue
        assert(capturedQuery.from == LocalDate.now().minusDays(2))
        assert(capturedQuery.to == LocalDate.now())
        assert(capturedQuery.topN == 5L)
    }
} 