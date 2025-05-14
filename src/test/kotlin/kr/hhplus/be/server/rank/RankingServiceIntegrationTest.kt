package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@SpringBootTest
class RankingServiceIntegrationTest @Autowired constructor(
    private val rankingService: RankingService,
    private val productRankingRepository: ProductRankingRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingKeyGenerator: RankingKeyGenerator,
    private val redisCleaner: RedisCleaner,
    @Autowired private val productRepository: ProductRepository
) {
    @MockitoSpyBean
    private lateinit var spyProductRepository: ProductRepository

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
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val key = rankingKeyGenerator.generateDailyKey(date)
            val score1 = redisTemplate.opsForZSet().score(key, productId1)
            val score2 = redisTemplate.opsForZSet().score(key, productId2)

            score1 shouldBe 3.0
            score2 shouldBe 2.0

            // 상위 랭킹 확인
            val topProducts = productRankingRepository.getTopN(date, date, 2)
            topProducts shouldContainExactly listOf(productId1, productId2)
        }
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
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val key = rankingKeyGenerator.generateDailyKey(date)
            val score = redisTemplate.opsForZSet().score(key, productId)
            score shouldBe 5.0
        }
    }

    @Test
    fun `✅상품 랭킹이 정확한 순서로 조회되어야 한다`() {
        // given
        val now = LocalDateTime.now()
        val date = now.toLocalDate()
        
        // 상품 데이터 저장
        val product1 = productRepository.save(ProductTestFixture.product(name = "상품1").build())
        val product2 = productRepository.save(ProductTestFixture.product(name = "상품2").build())

        // 랭킹 데이터 업데이트
        val command = RankingCommand.UpdateProductRanking.Root(
            items = listOf(
                RankingCommand.UpdateProductRanking.Item(product1.id!!, 3L),
                RankingCommand.UpdateProductRanking.Item(product2.id!!, 5L)
            ),
            timestamp = now
        )
        rankingService.updateProductRanking(command)

        Thread.sleep(500)

        // when
        val query = RankingQuery.RetrieveProductRanking(
            from = date,
            to = date,
            topN = 2
        )
        val result = rankingService.retrieveProductRanking(query)

        // then
        result.products.size shouldBe 2
        result.products[0].run {
            rank shouldBe 1
            productId shouldBe product2.id
            name shouldBe "상품2"
        }
        result.products[1].run {
            rank shouldBe 2
            productId shouldBe product1.id
            name shouldBe "상품1"
        }
    }

    @Test
    fun `✅캐시된 데이터는 DB 조회를 하지 않아야 한다`() {
        // given
        val now = LocalDateTime.now()
        val date = now.toLocalDate()

        // 상품 데이터 저장
        val product1 = productRepository.save(ProductTestFixture.product(name = "상품1").build())
        val product2 = productRepository.save(ProductTestFixture.product(name = "상품2").build())

        // 랭킹 데이터 업데이트
        val command = RankingCommand.UpdateProductRanking.Root(
            items = listOf(
                RankingCommand.UpdateProductRanking.Item(product1.id!!, 3L),
                RankingCommand.UpdateProductRanking.Item(product2.id!!, 5L)
            ),
            timestamp = now
        )
        rankingService.updateProductRanking(command)

        Thread.sleep(500)

        val query = RankingQuery.RetrieveProductRanking(
            from = date,
            to = date,
            topN = 2
        )

        // when
        // 첫 번째 호출
        rankingService.retrieveProductRanking(query)
        // 두 번째 호출 (캐시된 데이터 사용)
        rankingService.retrieveProductRanking(query)

        // then
        // findAll은 한 번만 호출되어야 함
        verify(spyProductRepository, times(1)).findAll(any())
    }
} 