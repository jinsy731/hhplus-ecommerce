package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime

@SpringBootTest
class RankingServiceIntegrationTest @Autowired constructor(
    private val rankingService: RankingService,
    private val productRankingRepository: ProductRankingRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingKeyGenerator: RankingKeyGenerator,
    private val redisCleaner: RedisCleaner
) {

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }

    @Test
    fun `✅상품 랭킹이 정상적으로 업데이트되어야 한다`() {
        // given
        val now = LocalDateTime.now()
        val date = now.toLocalDate()
        val productId1 = 1L
        val productId2 = 2L
        
        val command = RankingCommand.UpdateProductRanking.Root(
            items = listOf(
                RankingCommand.UpdateProductRanking.Item(productId1, 3L),
                RankingCommand.UpdateProductRanking.Item(productId2, 2L)
            ),
            timestamp = now
        )

        // when
        rankingService.updateProductRanking(command)

        // then
        val key = rankingKeyGenerator.generateDailyKey(date)
        val score1 = redisTemplate.opsForZSet().score(key, productId1)
        val score2 = redisTemplate.opsForZSet().score(key, productId2)
        
        score1 shouldBe 3.0
        score2 shouldBe 2.0

        // 상위 랭킹 확인
        val topProducts = productRankingRepository.getTopN(date, date, 2)
        topProducts shouldContainExactly listOf(productId1, productId2)
    }

    @Test
    fun `✅동일한 상품에 대한 랭킹 업데이트는 누적되어야 한다`() {
        // given
        val now = LocalDateTime.now()
        val date = now.toLocalDate()
        val productId = 1L
        
        val command1 = RankingCommand.UpdateProductRanking.Root(
            items = listOf(RankingCommand.UpdateProductRanking.Item(productId, 3L)),
            timestamp = now
        )
        
        val command2 = RankingCommand.UpdateProductRanking.Root(
            items = listOf(RankingCommand.UpdateProductRanking.Item(productId, 2L)),
            timestamp = now
        )

        // when
        rankingService.updateProductRanking(command1)
        rankingService.updateProductRanking(command2)

        // then
        val key = rankingKeyGenerator.generateDailyKey(date)
        val score = redisTemplate.opsForZSet().score(key, productId)
        score shouldBe 5.0
    }
} 