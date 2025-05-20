package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

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
}