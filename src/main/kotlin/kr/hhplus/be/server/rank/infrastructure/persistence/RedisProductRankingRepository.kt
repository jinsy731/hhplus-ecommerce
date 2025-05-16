package kr.hhplus.be.server.rank.infrastructure.persistence

import kr.hhplus.be.server.rank.application.RankingKeyGenerator
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class RedisProductRankingRepository(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingKeyGenerator: RankingKeyGenerator
    ): ProductRankingRepository {
    override fun increaseRanking(date: LocalDate, productId: Long, quantity: Int) {
        val key = rankingKeyGenerator.generateDailyKey(date)
        redisTemplate.opsForZSet().incrementScore(key, productId, quantity.toDouble())
    }

    override fun getTopN(
        from: LocalDate,
        to: LocalDate,
        topN: Long
    ): List<Long> {
        val keys = getInclusiveDateList(from, to)
            .map { rankingKeyGenerator.generateDailyKey(it) }
        val unionKey = rankingKeyGenerator.generateUnionKey(from, to)

        redisTemplate.opsForZSet()
            .unionAndStore(keys[0],keys.drop(1), unionKey)

        return redisTemplate.opsForZSet()
            .reverseRange(unionKey, 0, topN - 1)
            ?.map { (it as Int).toLong()}
            ?: emptyList()
    }

    fun getInclusiveDateList(from: LocalDate, to: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = from
        while (!current.isAfter(to)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }
}