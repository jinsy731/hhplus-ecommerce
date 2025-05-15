package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.rank.application.RankingKeyGenerator
import kr.hhplus.be.server.rank.infrastructure.persistence.RedisProductRankingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate

@SpringBootTest
class RedisProductRankingRepositoryTestIT @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingKeyGenerator: RankingKeyGenerator,
    private val productRankingRepository: RedisProductRankingRepository,
    private val redisCleaner: RedisCleaner
){
    @BeforeEach
    fun setUp() {
        // 테스트 전 Redis 데이터 초기화
        val keys = redisTemplate.keys("ranking:*")
        if (!keys.isNullOrEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }

    @Test
    fun `상품 랭킹이 정상적으로 증가한다`() {
        // given
        val today = LocalDate.now()
        val productId = 1L
        val quantity = 5

        // when
        productRankingRepository.increaseRanking(today, productId, quantity)

        // then
        val key = rankingKeyGenerator.generateDailyKey(today)
        val score = redisTemplate.opsForZSet().score(key, productId)
        score shouldBe quantity.toDouble()
    }

    @Test
    fun `여러 상품의 랭킹이 정확하게 집계된다`() {
        // given
        val today = LocalDate.now()
        val product1 = 1L to 5
        val product2 = 2L to 3
        val product3 = 3L to 8

        // when
        listOf(product1, product2, product3).forEach { (productId, quantity) ->
            productRankingRepository.increaseRanking(today, productId, quantity)
        }

        // then
        val topProducts = productRankingRepository.getTopN(today, today, 3)
        topProducts shouldContainExactly listOf(3L, 1L, 2L) // 8 > 5 > 3 순으로 정렬되어야 함
    }

    @Test
    fun `기간 내 집계가 정상적으로 동작한다`() {
        // given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(2)
        
        // 첫째 날: 상품 1이 5개
        productRankingRepository.increaseRanking(startDate, 1L, 5)
        
        // 둘째 날: 상품 2가 7개
        productRankingRepository.increaseRanking(startDate.plusDays(1), 2L, 7)
        
        // 셋째 날: 상품 1이 3개 추가
        productRankingRepository.increaseRanking(endDate, 1L, 3)

        // when
        val topProducts = productRankingRepository.getTopN(startDate, endDate, 2)

        // then
        // 상품 1: 총 8개 (5 + 3), 상품 2: 총 7개
        topProducts shouldContainExactly listOf(1L, 2L)
    }

    @Test
    fun `날짜 범위가 정상적으로 생성된다`() {
        // given
        val startDate = LocalDate.of(2024, 3, 1)
        val endDate = LocalDate.of(2024, 3, 3)

        // when
        val dates = productRankingRepository.getInclusiveDateList(startDate, endDate)

        // then
        dates shouldContainExactly listOf(
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 2),
            LocalDate.of(2024, 3, 3)
        )
    }
} 