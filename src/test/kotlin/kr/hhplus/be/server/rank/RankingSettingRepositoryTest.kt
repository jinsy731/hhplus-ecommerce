package kr.hhplus.be.server.rank

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class RankingSettingRepositoryTestIT @Autowired constructor(
    private val rankingSettingRepository: RankingSettingRepository,
    private val redisCleaner: RedisCleaner
) {

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }

    @Test
    fun `✅ RankingSetting 저장 및 조회가 정상적으로 동작한다`() {
        // given
        val periodType = RankingPeriod.DAILY
        val setting = RankingSetting(topN = 10L)

        // when
        rankingSettingRepository.save(periodType, setting)
        val retrievedSetting = rankingSettingRepository.get(periodType)

        // then
        retrievedSetting shouldBe setting
    }

    @Test
    fun `✅ 저장되지 않은 RankingPeriod 조회 시 null을 반환한다`() {
        // given
        val periodType = RankingPeriod.WEEKLY

        // when
        val retrievedSetting = rankingSettingRepository.get(periodType)

        // then
        retrievedSetting shouldBe null
    }

    @Test
    fun `✅ RankingSetting 덮어쓰기가 정상적으로 동작한다`() {
        // given
        val periodType = RankingPeriod.MONTHLY
        val initialSetting = RankingSetting(topN = 5L)
        val newSetting = RankingSetting(topN = 15L)

        // when
        rankingSettingRepository.save(periodType, initialSetting)
        rankingSettingRepository.save(periodType, newSetting)
        val retrievedSetting = rankingSettingRepository.get(periodType)

        // then
        retrievedSetting shouldBe newSetting
    }
} 