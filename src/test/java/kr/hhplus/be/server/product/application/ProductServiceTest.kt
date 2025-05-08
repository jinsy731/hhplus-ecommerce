package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val mockPopularProductDailyRepository = mockk<JpaPopularProductsDailyRepository>()
    private val mockStringRedisTemplate = mockk<StringRedisTemplate>()
    private val mockProductResultRedisTemplate = mockk<RedisTemplate<String, ProductResult.RetrieveList>>()
    private val productService = ProductService(
        productRepository,
        mockPopularProductDailyRepository,
        mockStringRedisTemplate,
        mockProductResultRedisTemplate,
        )

    @AfterEach
    fun tearDown() {
        clearMocks(mockProductResultRedisTemplate)
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
    fun `✅상품 목록 조회(캐싱)_캐시 miss인 경우 DB를 조회해야한다`() {
        // arrange
        val pageable = PageRequest.of(1, 10)
        val cmd = ProductCommand.RetrieveList(pageable, 1, "keyword")
        val products = listOf(
            ProductTestFixture.createValidProduct(1L, variantIds = listOf(1,2)),
            ProductTestFixture.createValidProduct(2L, variantIds = listOf(3,4)))
        val productListDto = products.map { ProductListDto(it.id!!, it.name, it.basePrice, it.status) }

        every { mockProductResultRedisTemplate.opsForValue() } returns mockk<ValueOperations<String, ProductResult.RetrieveList>>()
        val valueOps = mockProductResultRedisTemplate.opsForValue()
        every { valueOps.get(any()) } returns null
        every { valueOps.set(any(), any(), Duration.ofMinutes(10)) } just Runs
        every { productRepository.searchByKeyword(any(), any(), any()) } returns productListDto

        // act
        val result = productService.retrieveListWithPageCache(cmd)

        // assert
        result.products shouldHaveSize 2
        verify(exactly = 1) { valueOps.get(any()) }
        verify(exactly = 1) { valueOps.set(any(), any(), Duration.ofMinutes(10)) }
        verify(exactly = 1) { productRepository.searchByKeyword(any(), any(), any())}
    }

    @Test
    fun `✅상품 목록 조회(캐싱)_캐시 hit인 경우 DB를 조회하지 않는다`() {
        // arrange
        val pageable = PageRequest.of(1, 10)
        val cmd = ProductCommand.RetrieveList(pageable, 1, "keyword")
        val products = listOf(
            ProductTestFixture.createValidProduct(1L, variantIds = listOf(1,2)),
            ProductTestFixture.createValidProduct(2L, variantIds = listOf(3,4)))
        val productListDto = products.map { ProductListDto(it.id!!, it.name, it.basePrice, it.status) }
        val cached = ProductResult.RetrieveList(products = productListDto.map { it.toProductDetail() })

        every { mockProductResultRedisTemplate.opsForValue() } returns mockk<ValueOperations<String, ProductResult.RetrieveList>>()
        val valueOps = mockProductResultRedisTemplate.opsForValue()
        every { valueOps.get(any()) } returns cached
        every { valueOps.set(any(), any(), Duration.ofMinutes(10)) } just Runs
        every { productRepository.searchByKeyword(any(), any(), any()) } returns productListDto

        // act
        val result = productService.retrieveListWithPageCache(cmd)

        // assert
        result.products shouldHaveSize 2
        verify(exactly = 1) { valueOps.get(any()) }
        verify(exactly = 0) { valueOps.set(any(), any(), Duration.ofMinutes(10)) }
        verify(exactly = 0) { productRepository.searchByKeyword(any(), any(), any())}
    }

}