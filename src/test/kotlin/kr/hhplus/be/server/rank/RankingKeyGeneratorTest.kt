package kr.hhplus.be.server.rank

import kr.hhplus.be.server.rank.application.RankingKeyGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class RankingKeyGeneratorTest {
    private val rankingKeyGenerator = RankingKeyGenerator()

    @Test
    fun `generateDailyKey는 주어진 날짜에 대한 올바른 형식의 키를 생성한다`() {
        // given
        val date = LocalDate.of(2024, 3, 15)

        // when
        val key = rankingKeyGenerator.generateDailyKey(date)

        // then
        assertEquals("ranking:product:daily:20240315", key)
    }

    @Test
    fun `generateUnionKey는 시작일과 종료일에 대한 올바른 형식의 키를 생성한다`() {
        // given
        val fromDate = LocalDate.of(2024, 3, 1)
        val toDate = LocalDate.of(2024, 3, 15)

        // when
        val key = rankingKeyGenerator.generateUnionKey(fromDate, toDate)

        // then
        assertEquals("ranking:product:union:20240301:20240315", key)
    }
} 