package kr.hhplus.be.server.rank

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RankingServiceTest {
    private lateinit var rankingService: RankingService
    private lateinit var productRankingRepository: ProductRankingRepository
    private lateinit var productRepository: ProductRepository

    @BeforeEach
    fun setUp() {
        productRankingRepository = mockk()
        productRepository = mockk()
        rankingService = RankingService(productRankingRepository, productRepository)
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
} 