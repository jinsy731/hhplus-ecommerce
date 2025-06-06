package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.model.ProductRepository
import kr.hhplus.be.server.rank.application.RankingPeriod
import kr.hhplus.be.server.rank.application.RankingQuery
import kr.hhplus.be.server.rank.application.RankingResult
import kr.hhplus.be.server.rank.application.RankingService
import kr.hhplus.be.server.rank.infrastructure.persistence.ProductRankingRepository
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSetting
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSettingRepository
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
    private lateinit var rankingSettingRepository: RankingSettingRepository
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        productRankingRepository = mockk()
        productRepository = mockk()
        rankingSettingRepository = mockk(relaxed = true)
        cacheManager = mockk()
        rankingService =
            RankingService(productRankingRepository, productRepository, rankingSettingRepository, cacheManager)
    }

    @Test
    fun `상품 랭킹을 조회하면 순위대로 정렬된 결과를 반환한다`() {
        // given
        val periodType = RankingPeriod.DAILY
        val query = RankingQuery.RetrieveProductRanking(periodType)
        val today = LocalDate.now()
        val from = today.minusDays(periodType.periodDays)
        val topN = 5L

        val productIds = listOf(2L, 1L, 3L)
        val products = listOf(
            ProductTestFixture.product(id = 2L, name = "상품2").build(),
            ProductTestFixture.product(id = 1L, name = "상품1").build(),
            ProductTestFixture.product(id = 3L, name = "상품3").build(),
        )
        
        every { rankingSettingRepository.get(periodType) } returns RankingSetting(topN)
        every { productRankingRepository.getTopN(from, today, topN) } returns productIds
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

        verify(exactly = 1) { rankingSettingRepository.get(periodType) }
        verify(exactly = 1) { productRankingRepository.getTopN(from, today, topN) }
        verify(exactly = 1) { productRepository.findAll(productIds) }
    }

    @Test
    fun `renewProductRankingCache는 캐시를 갱신해야 한다`() {
        // given
        val periodType = RankingPeriod.DAILY
        val query = RankingQuery.RetrieveProductRanking(periodType)
        val today = LocalDate.now()
        val from = today.minusDays(periodType.periodDays)
        val topN = 2L
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY_PREFIX + periodType.name

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
        every { cacheManager.getCache(cacheName) } returns cache
        every { rankingSettingRepository.get(periodType) } returns RankingSetting(topN)
        every { productRankingRepository.getTopN(from, today, topN) } returns productIds
        every { productRepository.findAll(productIds) } returns products

        // when
        rankingService.renewProductRankingCache(query)

        // then
        verify(exactly = 1) { cacheManager.getCache(cacheName) }
        verify(exactly = 1) { rankingSettingRepository.get(periodType) }
        verify(exactly = 1) { productRankingRepository.getTopN(from, today, topN) }
        verify(exactly = 1) { productRepository.findAll(productIds) }
        verify(exactly = 1) { cache.put(cacheKey, expectedRankingResult) }
    }

    @Test
    fun `renewProductRankingCache는 캐시가 없으면 아무것도 하지 않아야 한다`() {
        // given
        val periodType = RankingPeriod.DAILY
        val query = RankingQuery.RetrieveProductRanking(periodType)
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME

        every { cacheManager.getCache(cacheName) } returns null

        // when
        rankingService.renewProductRankingCache(query)

        // then
        verify(exactly = 1) { cacheManager.getCache(cacheName) }
        verify(exactly = 0) { rankingSettingRepository.get(any()) }
        verify(exactly = 0) { productRankingRepository.getTopN(any(), any(), any()) }
        verify(exactly = 0) { productRepository.findAll(any<List<Long>>()) }
    }

    @Test
    fun `resolveQueryProperties는 RankingSetting이 존재할 때 해당 설정값을 반환한다`() {
        // given
        val periodType = RankingPeriod.DAILY
        val expectedTopN = 10L
        val setting = RankingSetting(expectedTopN)
        val today = LocalDate.now()
        val expectedFrom = today.minusDays(periodType.periodDays)

        every { rankingSettingRepository.get(periodType) } returns setting

        // when
        val (from, to, topN) = rankingService.resolveQueryProperties(periodType)

        // then
        from shouldBe expectedFrom
        to shouldBe today
        topN shouldBe expectedTopN
        verify(exactly = 1) { rankingSettingRepository.get(periodType) }
        verify(exactly = 0) { rankingSettingRepository.save(any(), any()) }
    }

    @Test
    fun `resolveQueryProperties는 RankingSetting이 없을 때 기본값으로 설정하고 반환한다`() {
        // given
        val periodType = RankingPeriod.WEEKLY
        val defaultTopN = 5L
        val today = LocalDate.now()
        val expectedFrom = today.minusDays(periodType.periodDays)

        every { rankingSettingRepository.get(periodType) } returns null
        every { rankingSettingRepository.save(periodType, RankingSetting(defaultTopN)) } returns RankingSetting(
            defaultTopN
        )

        // when
        val (from, to, topN) = rankingService.resolveQueryProperties(periodType)

        // then
        from shouldBe expectedFrom
        to shouldBe today
        topN shouldBe defaultTopN
        verify(exactly = 1) { rankingSettingRepository.get(periodType) }
        verify(exactly = 1) { rankingSettingRepository.save(periodType, RankingSetting(defaultTopN)) }
    }
} 