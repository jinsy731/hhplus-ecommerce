package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.shared.cache.CacheKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.LocalDate

class RankingServiceTest {
    private lateinit var rankingService: RankingService
    private lateinit var productRankingRepository: ProductRankingRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        productRankingRepository = mockk()
        productRepository = mockk()
        cacheManager = mockk()
        rankingService = RankingService(productRankingRepository, productRepository, cacheManager)
    }

    @Test
    fun `상품 랭킹을 조회하면 순위대로 정렬된 결과를 반환한다`() {
        // given
        val from = LocalDate.now()
        val to = LocalDate.now()
        val topN = 3L
        val query = RankingQuery.RetrieveProductRanking(from, to, topN)

        val productIds = listOf(2L, 1L, 3L)
        val products = listOf(
            ProductTestFixture.product(id = 2L, name = "상품2").build(),
            ProductTestFixture.product(id = 1L, name = "상품1").build(),
            ProductTestFixture.product(id = 3L, name = "상품3").build(),
        )

        every { productRankingRepository.getTopN(from, to, topN) } returns productIds
        every { productRepository.findAll(productIds) } returns products

        // when
        val result = rankingService.retrieveProductRanking(query)

        // then
        result.products.shouldHaveSize(3)
        
        result.products[0].run {
            rank shouldBe 1
            productId shouldBe 2L
            name shouldBe "상품2"
        }

        result.products[1].run {
            rank shouldBe 2
            productId shouldBe 1L
            name shouldBe "상품1"
        }

        result.products[2].run {
            rank shouldBe 3
            productId shouldBe 3L
            name shouldBe "상품3"
        }

        verify(exactly = 1) { productRankingRepository.getTopN(from, to, topN) }
        verify(exactly = 1) { productRepository.findAll(productIds) }
    }

    @Test
    fun `renewProductRankingCache는 캐시를 갱신해야 한다`() {
        // given
        val from = LocalDate.now()
        val to = LocalDate.now()
        val topN = 2L
        val query = RankingQuery.RetrieveProductRanking(from, to, topN)

        val productIds = listOf(2L, 1L)
        val products = listOf(
            ProductTestFixture.product(id = 2L, name = "상품2").build(),
            ProductTestFixture.product(id = 1L, name = "상품1").build()
        )
        val expectedRankingResult = RankingResult.RetrieveProductRanking.Root(
            products = listOf(
                RankingResult.RetrieveProductRanking.ProductRanking(1, 2L, "상품2"),
                RankingResult.RetrieveProductRanking.ProductRanking(2, 1L, "상품1")
            )
        )

        val cache = mockk<Cache>(relaxed = true)
        every { cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME) } returns cache
        every { productRankingRepository.getTopN(from, to, topN) } returns productIds
        every { productRepository.findAll(productIds) } returns products

        // when
        rankingService.renewProductRankingCache(query)

        // then
        verify(exactly = 1) { cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME) }
        verify(exactly = 1) { cache.put(CacheKey.PRODUCT_RANKING_CACHE_KEY, expectedRankingResult) }
    }

    @Test
    fun `renewProductRankingCache는 캐시가 없으면 아무것도 하지 않아야 한다`() {
        // given
        val from = LocalDate.now()
        val to = LocalDate.now()
        val topN = 2L
        val query = RankingQuery.RetrieveProductRanking(from, to, topN)

        every { cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME) } returns null

        // when
        rankingService.renewProductRankingCache(query)

        // then
        verify(exactly = 1) { cacheManager.getCache(CacheKey.PRODUCT_RANKING_CACHE_NAME) }
        verify(exactly = 0) { productRankingRepository.getTopN(any(), any(), any()) }
        verify(exactly = 0) { productRepository.findAll(any<List<Long>>()) }
    }
} 