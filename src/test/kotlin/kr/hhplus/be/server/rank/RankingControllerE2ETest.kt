package kr.hhplus.be.server.rank

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.rank.application.RankingCommand
import kr.hhplus.be.server.rank.application.RankingPeriod
import kr.hhplus.be.server.rank.application.RankingService
import kr.hhplus.be.server.rank.entrypoint.http.RankingResponse
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
        val rankingPeriod = RankingPeriod.DAILY
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY_PREFIX + rankingPeriod.name
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME

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
                timestamp = LocalDateTime.now()
            ))

        // act
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            // 캐시 초기화
            val cacheKey = "product:${rankingPeriod.name}"
            cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME)?.evict(cacheKey)

            val response = restTemplate.exchange(
                "/api/v1/ranking/products?periodType=$rankingPeriod",
                HttpMethod.GET,
                null,
                object: ParameterizedTypeReference<CommonResponse<RankingResponse.RetrieveTopProducts.Root>>() {})

            // assert
            response.statusCode.value() shouldBe 200
            response.body!!.data!!.topProducts.size shouldBe 3
            response.body!!.data!!.topProducts[0].rank shouldBe 1
            response.body!!.data!!.topProducts[0].name shouldBe "상품2"
            response.body!!.data!!.topProducts[0].productId shouldBe products[1].id!!
        }
    }
} 