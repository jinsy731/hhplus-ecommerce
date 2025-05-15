package kr.hhplus.be.server.rank

import kr.hhplus.be.server.rank.application.RankingKeyGenerator
import kr.hhplus.be.server.rank.infrastructure.persistence.RedisProductRankingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisProductRankingRepositoryTest {

    @Autowired
    private lateinit var redisProductRankingRepository: RedisProductRankingRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Autowired
    private lateinit var rankingKeyGenerator: RankingKeyGenerator

    companion object {
        @Container
        val redisContainer = GenericContainer<Nothing>(DockerImageName.parse("redis:latest")).apply {
            withExposedPorts(6379)
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379).toString() }
        }
    }

    @AfterEach
    fun tearDown() {
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Test
    @DisplayName("increaseRanking은 일별 상품 판매량을 증가시킨다")
    fun increaseRanking_increasesDailyProductSales() {
        // given
        val date = LocalDate.of(2024, 7, 25)
        val productId = 1L
        val quantity = 5

        // when
        redisProductRankingRepository.increaseRanking(date, productId, quantity)

        // then
        val key = rankingKeyGenerator.generateDailyKey(date)
        val score = redisTemplate.opsForZSet().score(key, productId)
        assertThat(score).isEqualTo(quantity.toDouble())
    }

    @Test
    @DisplayName("getTopN은 주어진 기간 동안 가장 많이 팔린 상품 ID 목록을 반환한다")
    fun getTopN_returnsTopNProductIdsForGivenPeriod() {
        // given
        val from = LocalDate.of(2024, 7, 23)
        val to = LocalDate.of(2024, 7, 25)
        val topN = 3L

        // 날짜별 상품 판매량 증가
        // 2024-07-23
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 23), 1L, 10)
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 23), 2L, 5)
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 23), 3L, 12)

        // 2024-07-24
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 24), 1L, 3)
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 24), 2L, 8) // 2L: 5 + 8 = 13
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 24), 4L, 7)

        // 2024-07-25
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 25), 3L, 6) // 3L: 12 + 6 = 18
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 25), 1L, 9) // 1L: 10 + 3 + 9 = 22
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 25), 5L, 15)

        // when
        val topProducts = redisProductRankingRepository.getTopN(from, to, topN)

        // then
        // 예상 결과: 1L (22), 3L (18), 5L (15)
        assertThat(topProducts).hasSize(topN.toInt())
        assertThat(topProducts).containsExactly(1L, 3L, 5L)
    }

    @Test
    @DisplayName("getTopN은 데이터가 없을 때 빈 리스트를 반환한다")
    fun getTopN_returnsEmptyListWhenNoData() {
        // given
        val from = LocalDate.of(2024, 7, 20)
        val to = LocalDate.of(2024, 7, 22)
        val topN = 3L

        // when
        val topProducts = redisProductRankingRepository.getTopN(from, to, topN)

        // then
        assertThat(topProducts).isEmpty()
    }

    @Test
    @DisplayName("getTopN은 요청한 topN보다 적은 수의 상품이 있을 경우, 존재하는 상품만큼만 반환한다")
    fun getTopN_returnsAvailableProductsWhenLessThanTopN() {
        // given
        val from = LocalDate.of(2024, 7, 26)
        val to = LocalDate.of(2024, 7, 26)
        val topN = 5L

        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 26), 10L, 100)
        redisProductRankingRepository.increaseRanking(LocalDate.of(2024, 7, 26), 11L, 50)

        // when
        val topProducts = redisProductRankingRepository.getTopN(from, to, topN)

        // then
        assertThat(topProducts).hasSize(2)
        assertThat(topProducts).containsExactly(10L, 11L)
    }
} 