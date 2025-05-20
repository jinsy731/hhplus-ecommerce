package kr.hhplus.be.server.product.application

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.executeConcurrently
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductServiceConcurrencyTestIT @Autowired constructor(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantJpaRepository
) {
    
    @Test
    fun `✅재고 감소 동시성 테스트_재고감소 요청을 동시에 보내도 재고가 정확히 반영되어야 한다`() {
        // arrange: 5개의 상품 설정, 각 상품은 2개의 variant를 가지고 있고 각 variant는 100개의 재고를 가짐
        val variantIds = mutableListOf<Long>()
        val items = mutableListOf<ProductCommand.ValidateAndReduceStock.Item>()
        val quantity = 1
        repeat(5) {
            val product = ProductTestFixture
                .product()
                .withVariants(ProductTestFixture.variant(stock = 100), ProductTestFixture.variant(stock = 100))
                .build()

            val savedProduct = productRepository.save(product)
            variantIds.addAll(savedProduct.variants.map { it.id!! })
            items.add(
                ProductCommand.ValidateAndReduceStock.Item(
                productId = savedProduct.id!!,
                variantId = savedProduct.variants[0].id!!,
                quantity = quantity
            ))
            items.add(
                ProductCommand.ValidateAndReduceStock.Item(
                productId = savedProduct.id!!,
                variantId = savedProduct.variants[1].id!!,
                quantity = quantity
            ))
        }
        val cmd = ProductCommand.ValidateAndReduceStock.Root(items = items)

        // act: 100개의 동시 요청
        executeConcurrently(100) {
            productService.validateAndReduceStock(cmd)
        }
        
        // assert: 재고는 정확히 0이어야 함.
        val variants = productVariantRepository.findAllById(variantIds)
        variants.forEach { v -> v.stock shouldBe 0 }
    }
    
}