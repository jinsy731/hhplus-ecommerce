package kr.hhplus.be.server.product.infrastructure

import io.kotest.matchers.collections.shouldHaveSize
import jakarta.transaction.Transactional
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class ProductRepositoryTestIT @Autowired constructor(
    private val productRepository: ProductRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
    private val txTemplate: TransactionTemplate
){
    private val logger = LoggerFactory.getLogger(javaClass)

    @AfterEach
    fun tearDown() {
        databaseCleaner.clean()
    }

    @Test
    fun `✅상품 리스트 조회(for update)`() {
        // arrange: 10개의 상품 등록, 각 상품은 2개의 Variants를 가짐.
        val productIds = mutableListOf<Long>()
        txTemplate.execute {
            repeat(10) {
                val product = ProductTestFixture
                    .product()
                    .withVariants(ProductTestFixture.variant(), ProductTestFixture.variant())
                    .build()
                val savedProduct = productRepository.save(product)
                productIds.add(savedProduct.id!!)
            }
        }

        // act: productIds로 상품을 조회
        logger.info { "productIds = $productIds" }
        val findProducts = txTemplate.execute {
            productRepository.findAllByIdForUpdate(productIds)
        }

        // assert: findProducts의 size는 10이어야 함.
        findProducts!! shouldHaveSize 10
    }
}