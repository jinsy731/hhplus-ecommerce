package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.rank.application.RankingCommand
import kr.hhplus.be.server.rank.application.RankingKeyGenerator
import kr.hhplus.be.server.rank.application.RankingPeriod
import kr.hhplus.be.server.rank.application.RankingQuery
import kr.hhplus.be.server.rank.application.RankingResult
import kr.hhplus.be.server.rank.application.RankingService
import kr.hhplus.be.server.rank.infrastructure.persistence.ProductRankingRepository
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSetting
import kr.hhplus.be.server.rank.infrastructure.persistence.RankingSettingRepository
import kr.hhplus.be.server.shared.cache.CacheKey
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@SpringBootTest
class RankingServiceTestIT @Autowired constructor(
    private val rankingService: RankingService,
    private val productRankingRepository: ProductRankingRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val rankingKeyGenerator: RankingKeyGenerator,
    private val redisCleaner: RedisCleaner,
    @Autowired private val productRepository: ProductRepository,
    @Autowired private val cacheManager: CacheManager,
    @Autowired private val rankingSettingRepository: RankingSettingRepository
) {
    @MockitoSpyBean
    private lateinit var spyProductRepository: ProductRepository

    @BeforeEach
    fun setup() {
        rankingSettingRepository.save(RankingPeriod.DAILY, RankingSetting(5L))
        rankingSettingRepository.save(RankingPeriod.WEEKLY, RankingSetting(5L))
        rankingSettingRepository.save(RankingPeriod.MONTHLY, RankingSetting(5L))
    }

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
        Mockito.reset(spyProductRepository)
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
        val periodType = RankingPeriod.DAILY

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
        val query = RankingQuery.RetrieveProductRanking(periodType)
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
        val periodType = RankingPeriod.DAILY
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY_PREFIX + periodType.name
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME
        val now = LocalDateTime.now()
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

        val query = RankingQuery.RetrieveProductRanking(periodType)

        // when : 두 번 호출, 두 번쨰는 캐시 타야함.
        Mockito.clearInvocations(spyProductRepository) // save 할 때 findAll이 호출돼서 테스트 간섭 생겨서 추가.. 왜?
        cacheManager.getCache(cacheName)?.evict(cacheKey)
        rankingService.retrieveProductRanking(query)
        val result = rankingService.retrieveProductRanking(query)

        // then
        verify(spyProductRepository, times(1)).findAll(any())
        result.products.size shouldBe 2
        result.products[0].name shouldBe "상품2"
        result.products[1].name shouldBe "상품1"
    }

    @Test
    fun `✅상품 랭킹 캐시가 정상적으로 갱신되어야 한다`() {
        // given
        val now = LocalDateTime.now()
        val date = now.toLocalDate()
        val cacheKey = CacheKey.PRODUCT_RANKING_CACHE_KEY_PREFIX + RankingPeriod.DAILY.name
        val cacheName = CacheKey.PRODUCT_RANKING_CACHE_NAME
        val periodType = RankingPeriod.DAILY

        // 상품 데이터 저장
        val product1 = productRepository.save(ProductTestFixture.product(name = "상품1").build())
        val product2 = productRepository.save(ProductTestFixture.product(name = "상품2").build())
        val product3 = productRepository.save(ProductTestFixture.product(name = "상품3").build())


        // 랭킹 데이터 업데이트
        val command = RankingCommand.UpdateProductRanking.Root(
            items = listOf(
                RankingCommand.UpdateProductRanking.Item(product1.id!!, 3L),
                RankingCommand.UpdateProductRanking.Item(product2.id!!, 5L),
                RankingCommand.UpdateProductRanking.Item(product3.id!!, 2L)
            ),
            timestamp = now
        )
        rankingService.updateProductRanking(command)

        Thread.sleep(500) // 비동기 처리 대기

        val query = RankingQuery.RetrieveProductRanking(periodType)
        rankingSettingRepository.save(periodType, RankingSetting(2L)) // 테스트용 topN 설정

        // when: 첫 번째 캐시 갱신 (상위 2개)
        rankingService.renewProductRankingCache(query)

        // then: 캐시된 데이터 확인
        val cache = cacheManager.getCache(cacheName)
        var cachedResult = cache?.get(cacheKey)?.get() as RankingResult.RetrieveProductRanking.Root?
        
        cachedResult shouldBe RankingResult.RetrieveProductRanking.Root(
            products = listOf(
                RankingResult.RetrieveProductRanking.ProductRanking(1, product2.id!!, "상품2"),
                RankingResult.RetrieveProductRanking.ProductRanking(2, product1.id!!, "상품1")
            )
        )

        // when: 다른 조건으로 캐시 갱신 (상위 3개)
        val queryTop3 = RankingQuery.RetrieveProductRanking(periodType)
        rankingSettingRepository.save(periodType, RankingSetting(3L)) // 테스트용 topN 변경

        rankingService.renewProductRankingCache(queryTop3)

        // then: 캐시된 데이터가 갱신되었는지 확인
        cachedResult = cache?.get(cacheKey)?.get() as RankingResult.RetrieveProductRanking.Root?
        cachedResult shouldBe RankingResult.RetrieveProductRanking.Root(
            products = listOf(
                RankingResult.RetrieveProductRanking.ProductRanking(1, product2.id!!, "상품2"),
                RankingResult.RetrieveProductRanking.ProductRanking(2, product1.id!!, "상품1"),
                RankingResult.RetrieveProductRanking.ProductRanking(3, product3.id!!, "상품3")
            )
        )
    }

    @Test
    fun `✅periodType별로 설정된 topN에 따라 다른 개수의 상품 랭킹이 조회되어야 한다`() {
        // given
        val now = LocalDateTime.now()

        // 상품 데이터 저장
        val products = (1..10).map {
            productRepository.save(ProductTestFixture.product(name = "상품$it").build())
        }

        // 랭킹 데이터 업데이트 (모든 상품에 대해 점수 부여)
        val commandItems = products.mapIndexed { index, product ->
            RankingCommand.UpdateProductRanking.Item(product.id!!, (10 - index).toLong()) // 상품10이 1점, 상품1이 10점
        }
        val command = RankingCommand.UpdateProductRanking.Root(
            items = commandItems,
            timestamp = now
        )
        rankingService.updateProductRanking(command)

        Thread.sleep(500) // 비동기 처리 대기

        // 각 periodType에 대한 topN 설정
        val dailyTopN = 3L
        val weeklyTopN = 5L
        val monthlyTopN = 7L
        rankingSettingRepository.save(RankingPeriod.DAILY, RankingSetting(dailyTopN))
        rankingSettingRepository.save(RankingPeriod.WEEKLY, RankingSetting(weeklyTopN))
        rankingSettingRepository.save(RankingPeriod.MONTHLY, RankingSetting(monthlyTopN))

        // when & then
        // DAILY
        val dailyQuery = RankingQuery.RetrieveProductRanking(periodType = RankingPeriod.DAILY)
        val dailyResult = rankingService.retrieveProductRanking(dailyQuery)
        dailyResult.products.size shouldBe dailyTopN.toInt()
        dailyResult.products.forEachIndexed { index, productRanking ->
            productRanking.rank shouldBe index + 1
            // 가장 점수가 높은 상품부터 순서대로 나오는지 확인 (상품1, 상품2, 상품3)
            productRanking.productId shouldBe products[index].id
        }

        // WEEKLY
        val weeklyQuery = RankingQuery.RetrieveProductRanking(periodType = RankingPeriod.WEEKLY)
        val weeklyResult = rankingService.retrieveProductRanking(weeklyQuery)
        weeklyResult.products.size shouldBe weeklyTopN.toInt()
        weeklyResult.products.forEachIndexed { index, productRanking ->
            productRanking.rank shouldBe index + 1
            productRanking.productId shouldBe products[index].id
        }

        // MONTHLY
        val monthlyQuery = RankingQuery.RetrieveProductRanking(periodType = RankingPeriod.MONTHLY)
        val monthlyResult = rankingService.retrieveProductRanking(monthlyQuery)
        monthlyResult.products.size shouldBe monthlyTopN.toInt()
        monthlyResult.products.forEachIndexed { index, productRanking ->
            productRanking.rank shouldBe index + 1
            productRanking.productId shouldBe products[index].id
        }
    }

    @Test
    fun `✅resolveQueryProperties는 저장된 설정에 따라 정확한 기간과 topN을 반환한다`() {
        val today = LocalDate.now()
        val initialTopN = 5L // @BeforeEach에서 설정된 값

        // DAILY - 초기 설정값 확인
        var (from, to, topN) = rankingService.resolveQueryProperties(RankingPeriod.DAILY)
        from shouldBe today.minusDays(RankingPeriod.DAILY.periodDays)
        to shouldBe today
        topN shouldBe initialTopN

        // WEEKLY - 초기 설정값 확인
        val (weeklyFrom, weeklyTo, weeklyTopN) = rankingService.resolveQueryProperties(RankingPeriod.WEEKLY)
        weeklyFrom shouldBe today.minusDays(RankingPeriod.WEEKLY.periodDays)
        weeklyTo shouldBe today
        weeklyTopN shouldBe initialTopN

        // MONTHLY - 초기 설정값 확인
        val (monthlyFrom, monthlyTo, monthlyTopN) = rankingService.resolveQueryProperties(RankingPeriod.MONTHLY)
        monthlyFrom shouldBe today.minusDays(RankingPeriod.MONTHLY.periodDays)
        monthlyTo shouldBe today
        monthlyTopN shouldBe initialTopN
    }

    @Test
    fun `✅resolveQueryProperties는 RankingSetting 변경 시 업데이트된 topN을 반환한다`() {
        val today = LocalDate.now()
        val initialTopN = 5L // @BeforeEach에서 설정된 값
        val updatedTopN = 10L
        val periodType = RankingPeriod.DAILY

        // DAILY - 설정 변경 전 확인
        var (from, to, topN) = rankingService.resolveQueryProperties(periodType)
        topN shouldBe initialTopN

        // DAILY - 설정 변경
        rankingSettingRepository.save(periodType, RankingSetting(updatedTopN))
        val (updatedFrom, updatedTo, newTopN) = rankingService.resolveQueryProperties(periodType)
        updatedFrom shouldBe today.minusDays(periodType.periodDays)
        updatedTo shouldBe today
        newTopN shouldBe updatedTopN

        // 테스트 후 원래 값으로 복원 (선택 사항, 다른 테스트에 영향 주지 않기 위함)
        rankingSettingRepository.save(periodType, RankingSetting(initialTopN))
    }
} 