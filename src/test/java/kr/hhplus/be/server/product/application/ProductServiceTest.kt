package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jodd.time.TimeUtil.fromDate
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.domain.stats.PopularProductsDaily
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

class ProductServiceTest {
    val productRepository = mockk<ProductRepository>()
    val popularProductsDailyRepository = mockk<JpaPopularProductsDailyRepository>()

    val productService = ProductService(productRepository, popularProductsDailyRepository)

    @AfterEach
    fun tearDown() {
        clearMocks(productRepository)
        clearMocks(popularProductsDailyRepository)
    }
    
    @Test
    fun `✅상품 목록 조회`() {
        // arrange
        val pageable = PageRequest.of(1, 10)
        val cmd = ProductCommand.RetrieveList(pageable, 1, "keyword")
        val products = listOf(
            ProductTestFixture.createValidProduct(1L, variantIds = listOf(1,2)),
            ProductTestFixture.createValidProduct(2L, variantIds = listOf(3,4)))
        val productListDto = listOf(ProductListDto(
            id = 1L,
            name = "aa",
            basePrice = Money.of(1000),
            status = ProductStatus.ON_SALE
        ))
        every { productRepository.searchByKeyword(any(), any(), any()) } returns productListDto

        // act
        val result = productService.retrieveList(cmd)

        // assert
        result.products shouldHaveSize 1
    }

    @Test
    fun `✅상품 목록 조회(캐싱)_캐시된 경우 DB를 조회하지 않아야 한다`() {
        every { productRepository.searchByKeyword(any(), any(), any()) } returns listOf(
            ProductListDto(1L, "coat A", Money(BigDecimal("10000")), ProductStatus.ON_SALE)
        )


        val retrieveListCmd = ProductCommand.RetrieveList(
            keyword = "코트",
            lastId = null,
            pageable = PageRequest.of(0, 5)
        )

        productService.retrieveListWithPageCache(retrieveListCmd)
        productService.retrieveListWithPageCache(retrieveListCmd)

        verify(exactly = 1) {
            productRepository.searchByKeyword(any(), any(), any())
        }
    }

    @Test
    fun `✅인기 상품 조회(캐싱)_캐싱된 경우 DB를 조회하지 않아야 한다`() {
        every { popularProductsDailyRepository.findAllById(any()) } returns listOf(
            PopularProductsDaily(PopularProductDailyId(LocalDate.now(), 1), 1L, 100L)
        )
        every { productRepository.findAll(any()) } returns listOf(ProductTestFixture.product(id = 1L).build())
        val fromDate = LocalDate.now()
        val toDate = LocalDate.now()
        val retrievePopularCmd = ProductCommand.RetrievePopularProducts(fromDate, toDate, 5)

        // 첫번째 호출 (Cache Miss)
        productService.retrievePopularWithCaching(retrievePopularCmd)

        // 두번째 호출 (Cache Hit)
        productService.retrievePopularWithCaching(retrievePopularCmd)

        verify(exactly = 1) { popularProductsDailyRepository.findAllById(any()) }
    }

}