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
    private val productJpaRepository: ProductJpaRepository,
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

    @Test
    fun `상품 검색 Id 목록 조회`() {
        // arrange: 검색 조건에 부합하는 상품 2개, 부합하지 않는 상품 3개 설정
        val products = listOf(
            ProductTestFixture.product(name = "테스트 상품1").build(),
            ProductTestFixture.product(name = "테스트 상품2").build(),
            ProductTestFixture.product(name = "dummy1").build(),
            ProductTestFixture.product(name = "dummy2").build(),
            ProductTestFixture.product(name = "dummy2").build(),
        )
        productJpaRepository.saveAll(products)

        // act
        val ids = productRepository.searchIdsByKeyword("테스트")

        // assert
        ids shouldHaveSize 2
    }

    @Test
    fun `상품 조회 by ids`() {
        // arrange: 상품 10개 저장
        val products = IntRange(0, 9).map { ProductTestFixture.product().build() }
        val ids = productJpaRepository.saveAll(products).mapNotNull { it.id }

        // act: id로 상품 조회
        val productSummaries = productRepository.findSummaryByIds(ids)

        // assert: 조회된 상품이 10개인지 검증
        productSummaries shouldHaveSize 10
    }
}