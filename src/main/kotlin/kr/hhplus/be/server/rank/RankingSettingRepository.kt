package kr.hhplus.be.server.rank

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

interface RankingSettingRepository {
    fun get(periodType: RankingPeriod): RankingSetting?
    fun save(periodType: RankingPeriod, setting: RankingSetting): RankingSetting
}

@Repository
class RankingSettingRedisRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
): RankingSettingRepository {

    private val key = "ranking:setting"

    override fun get(periodType: RankingPeriod): RankingSetting? {
        val keyByPeriod = "${key}:${periodType}"
        val jsonString = redisTemplate.opsForValue().get(keyByPeriod) ?: return null
        return objectMapper.readValue(jsonString, RankingSetting::class.java)
    }

    override fun save(periodType: RankingPeriod, setting: RankingSetting): RankingSetting {
        val keyByPeriod = "${key}:${periodType}"
        val jsonString = objectMapper.writeValueAsString(setting)
        redisTemplate.opsForValue().set(keyByPeriod, jsonString)

        return setting
    }
}

data class RankingSetting(val topN: Long)