package kr.hhplus.be.server.rank

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.shared.cache.CacheKey
import kr.hhplus.be.server.shared.web.CommonResponse
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingControllerE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingService: RankingService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
    private val redisCleaner: RedisCleaner,
    private val cacheManager: CacheManager
) {
    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
        redisCleaner.clean()
    }
    @Test
    fun `retrieveTopProducts는 최근 3일 기준 상위 5개 인기 상품을 조회한다`() {
        // arrange: 상품 랭킹 세팅
        redisCleaner.clean()
        val product1 = ProductTestFixture.product(name = "상품1").build()
        val product2 = ProductTestFixture.product(name = "상품2").build()
        val product3 = ProductTestFixture.product(name = "상품3").build()
        val products = productJpaRepository.saveAll(listOf(product1, product2, product3))

        rankingService.updateProductRanking(
            RankingCommand.UpdateProductRanking.Root(
                items = listOf(
                    RankingCommand.UpdateProductRanking.Item(products[0].id!!, 3L),
                    RankingCommand.UpdateProductRanking.Item(products[1].id!!, 5L),
                    RankingCommand.UpdateProductRanking.Item(products[2].id!!, 2L)
                ),
                timestamp = LocalDateTime.of(2025, 5, 15, 0, 0, 0)
            ))

        // act
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME)?.evict(CacheKey.PRODUCT_RANKING_CACHE_KEY)
            val response = restTemplate.exchange(
                "/api/v1/ranking/products",
                HttpMethod.GET,
                null,
                object: ParameterizedTypeReference<CommonResponse<RankingResponse.RetrieveTopProducts.Root>>() {})

            redisTemplate.opsForZSet().rangeWithScores("ranking:product:daily:20250515", 0, -1)?.forEach { println("it = ${it}") }

            // assert
            response.body!!.data!!.topProducts.size shouldBe 3
            response.body!!.data!!.topProducts[0].rank shouldBe 1
            response.body!!.data!!.topProducts[0].name shouldBe "상품2"
            response.body!!.data!!.topProducts[0].productId shouldBe products[1].id!!
        }
    }
} 