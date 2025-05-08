package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val mockPopularProductDailyRepository = mockk<JpaPopularProductsDailyRepository>()
    private val productService = ProductService(productRepository, mockPopularProductDailyRepository)
    
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