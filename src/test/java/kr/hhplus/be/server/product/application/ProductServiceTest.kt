package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val productService = ProductService(productRepository)
    
    @Test
    fun `✅상품 목록 조회`() {
        // arrange
        val pageable = PageRequest.of(1, 10)
        val cmd = ProductCommand.RetrieveList(pageable, "keyword")
        val products = listOf(ProductTestFixture.createValidProduct(1L), ProductTestFixture.createValidProduct(2L))
        val page = PageImpl(products, cmd.pageable, 100)
        every { productRepository.searchByNameContaining(any(), any()) } returns page

        // act
        val result = productService.retrieveList(cmd)

        // assert
        result.products shouldHaveSize 2
        result.paginationResult.page shouldBe 1
        result.paginationResult.size shouldBe 10
        result.paginationResult.totalElements shouldBe 100
        result.paginationResult.totalPages shouldBe 10
    }

}