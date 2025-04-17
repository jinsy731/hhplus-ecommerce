package kr.hhplus.be.server.product.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import kr.hhplus.be.server.product.infrastructure.JpaProductSalesAggregationDailyRepository
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val productAggregationDailyRepository: JpaProductSalesAggregationDailyRepository = mockk()
    private val productService = ProductService(productRepository, productAggregationDailyRepository)
    
    @Test
    fun `✅상품 목록 조회`() {
        // arrange
        val pageable = PageRequest.of(1, 10)
        val cmd = ProductCommand.RetrieveList(pageable, "keyword")
        val products = listOf(
            ProductTestFixture.createValidProduct(1L, variantIds = listOf(1,2)),
            ProductTestFixture.createValidProduct(2L, variantIds = listOf(3,4)))
        val page = PageImpl(products, cmd.pageable, 100)
        every { productRepository.searchByNameContaining(any(), any()) } returns page

        // act
        val result = productService.retrieveList(cmd)

        // assert
        result.products shouldHaveSize 2
        result.pageResult.page shouldBe 1
        result.pageResult.size shouldBe 10
        result.pageResult.totalElements shouldBe 100
        result.pageResult.totalPages shouldBe 10
    }

}