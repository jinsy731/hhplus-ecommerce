package kr.hhplus.be.server.product.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.ProductUnavailableException
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import kr.hhplus.be.server.shared.exception.VariantOutOfStockException
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.domain.stats.PopularProductsDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.JpaProductSalesAggregationDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@SpringBootTest
class ProductServiceTestIT @Autowired constructor(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
    private val productVariantRepository: ProductVariantJpaRepository,
    private val productSalesAggregationDailyRepository: JpaProductSalesAggregationDailyRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository
) {
    private val testProducts = mutableListOf<Product>()

    @BeforeEach
    fun setup() {
        // 테스트 데이터 준비
        val product1 = Product(
            name = "테스트 상품 1",
            basePrice = Money.of(10000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(1000),
                    stock = 10
                )
            )
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(2000),
                    stock = 5
                )
            )
        }

        val product2 = Product(
            name = "테스트 상품 2",
            basePrice = Money.of(20000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(0),
                    stock = 8
                )
            )
        }

        val product3 = Product(
            name = "품절 상품",
            basePrice = Money.of(15000),
            status = ProductStatus.OUT_OF_STOCK
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(500),
                    stock = 0
                )
            )
        }

        val product4 = Product(
            name = "인기 상품 1",
            basePrice = Money.of(25000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(1500),
                    stock = 15
                )
            )
        }

        val product5 = Product(
            name = "인기 상품 2",
            basePrice = Money.of(30000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = Money.of(2500),
                    stock = 20
                )
            )
        }

        testProducts.addAll(listOf(
            productRepository.save(product1),
            productRepository.save(product2),
            productRepository.save(product3),
            productRepository.save(product4),
            productRepository.save(product5)
        ))
        
        // 판매량 데이터 추가
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val dayBeforeYesterday = today.minusDays(2)
        
        // 인기 상품 1 (총 판매량: 50)
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[3].id!!, today),
                salesCount = 20
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[3].id!!, yesterday),
                salesCount = 15
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[3].id!!, dayBeforeYesterday),
                salesCount = 15
            )
        )
        
        // 인기 상품 2 (총 판매량: 40)
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[4].id!!, today),
                salesCount = 15
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[4].id!!, yesterday),
                salesCount = 15
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[4].id!!, dayBeforeYesterday),
                salesCount = 10
            )
        )
        
        // 테스트 상품 1 (총 판매량: 30)
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[0].id!!, today),
                salesCount = 10
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[0].id!!, yesterday),
                salesCount = 10
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[0].id!!, dayBeforeYesterday),
                salesCount = 10
            )
        )
        
        // 테스트 상품 2 (총 판매량: 20)
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[1].id!!, today),
                salesCount = 5
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[1].id!!, yesterday),
                salesCount = 10
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[1].id!!, dayBeforeYesterday),
                salesCount = 5
            )
        )
        
        // 품절 상품 (총 판매량: 10)
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[2].id!!, yesterday),
                salesCount = 5
            )
        )
        productSalesAggregationDailyRepository.save(
            ProductSalesAggregationDaily(
                id = ProductSalesAggregationDailyId(testProducts[2].id!!, dayBeforeYesterday),
                salesCount = 5
            )
        )

        popularProductsDailyRepository.saveAll(listOf(
            PopularProductsDaily(
                id = PopularProductDailyId(LocalDate.now(), 1),
                productId = testProducts[3].id!!,
                totalSales = 1000
            ),
            PopularProductsDaily(
                id = PopularProductDailyId(LocalDate.now(), 2),
                productId = testProducts[4].id!!,
                totalSales = 500
            ),
            PopularProductsDaily(
                id = PopularProductDailyId(LocalDate.now(), 3),
                productId = testProducts[0].id!!,
                totalSales = 300
            ),
        ))
    }

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `✅상품 목록을 조회할 수 있다`() {
        // Arrange
        val pageable = PageRequest.of(0, 10)
        val cmd = ProductCommand.RetrieveList(pageable, null, "테스트")

        // Act
        val result = productService.retrieveList(cmd)

        // Assert
        result.products shouldHaveSize 2
        result.products.map { it.name } shouldContainExactlyInAnyOrder listOf("테스트 상품 1", "테스트 상품 2")
    }

    @Test
    fun `✅ID 목록으로 상품들을 조회할 수 있다`() {
        // Arrange
        val ids = testProducts.map { it.id!! }

        // Act
        val result = productService.findAllById(ids)

        // Assert
        result shouldHaveSize 5
        result.map { it.id } shouldContainExactlyInAnyOrder ids
    }

    @Test
    fun `✅구매 가능한 상품과 옵션은 검증을 통과한다`() {
        // Arrange
        val product = testProducts[0]
        val variantId = product.variants[0].id
        val cmd = ProductCommand.ValidateAndReduceStock.Root(
            listOf(
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = product.id!!,
                    variantId = variantId!!,
                    quantity = 5
                )
            )
        )

        // Act & Assert
        productService.validateAndReduceStock(cmd) // 예외가 발생하지 않아야 함
    }

    @Test
    fun `❌품절된 상품(status=OUT_OF_STOCK)은 구매 불가능하다`() {
        // Arrange
        val outOfStockProduct = testProducts[2]
        val variantId = outOfStockProduct.variants[0].id
        val cmd = ProductCommand.ValidateAndReduceStock.Root(
            listOf(
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = outOfStockProduct.id!!,
                    variantId = variantId!!,
                    quantity = 1
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<ProductUnavailableException> {
            productService.validateAndReduceStock(cmd)
        }
    }

    @Test
    fun `❌존재하지 않는 상품은 구매 검증 시 예외가 발생한다`() {
        // Arrange
        val nonExistentProductId = 9999L
        val cmd = ProductCommand.ValidateAndReduceStock.Root(
            listOf(
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = nonExistentProductId,
                    variantId = 1L,
                    quantity = 1
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<ResourceNotFoundException> {
            productService.validateAndReduceStock(cmd)
        }
    }

    @Test
    fun `❌주문 수량이 재고보다 많으면 예외가 발생한다`() {
        // Arrange
        val product = testProducts[0]
        val variant = product.variants[1] // 재고 5개
        val cmd = ProductCommand.ValidateAndReduceStock.Root(
            listOf(
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = product.id!!,
                    variantId = variant.id!!,
                    quantity = 10 // 재고보다 많은 수량
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<VariantOutOfStockException> {
            productService.validateAndReduceStock(cmd)
        }
    }

    @Test
    fun `✅구매로 재고를 감소시킬 수 있다`() {
        // Arrange
        val product = testProducts[0]
        val variant = product.variants[0]
        val initialStock = variant.stock
        val quantity = 3
        val cmd = ProductCommand.ValidateAndReduceStock.Root(
            listOf(
                ProductCommand.ValidateAndReduceStock.Item(
                    productId = product.id!!,
                    variantId = variant.id!!,
                    quantity = quantity
                )
            )
        )

        // Act
        productService.validateAndReduceStock(cmd)

        // Assert
        val updatedVariant = productVariantRepository.findById(variant.id!!).getOrNull() ?: throw IllegalStateException()
        updatedVariant.stock shouldBe (initialStock - quantity)
    }
    
    @Test
    fun `✅최근 3일간 인기상품을 판매량 순으로 조회할 수 있다`() {
        // Arrange
        val today = LocalDate.now()
        val fromDate = today.minusDays(2)
        val limit = 5
        
        val cmd = ProductCommand.RetrievePopularProducts(
            fromDate = fromDate,
            toDate = today,
            limit = limit
        )
        
        // Act
        val result = productService.retrievePopular(cmd)
        
        // Assert
        result shouldHaveSize 3
        
        // 판매량 순으로 정렬되어야 함
        result[0].productId shouldBe testProducts[3].id // 인기 상품 1 (판매량 50)
        result[0].name shouldBe "인기 상품 1"
        result[0].totalSales shouldBe 1000
        
        result[1].productId shouldBe testProducts[4].id // 인기 상품 2 (판매량 40)
        result[1].name shouldBe "인기 상품 2"
        result[1].totalSales shouldBe 500
        
        result[2].productId shouldBe testProducts[0].id // 테스트 상품 1 (판매량 30)
        result[2].name shouldBe "테스트 상품 1"
        result[2].totalSales shouldBe 300
    }
    
    @Test
    fun `✅인기상품 조회 시 limit 수만큼만 조회된다`() {
        // Arrange
        val today = LocalDate.now()
        val fromDate = today.minusDays(2)
        val limit = 3
        
        val cmd = ProductCommand.RetrievePopularProducts(
            fromDate = fromDate,
            toDate = today,
            limit = limit
        )
        
        // Act
        val result = productService.retrievePopular(cmd)
        
        // Assert
        result shouldHaveSize 3
        
        // 상위 3개 상품만 조회되어야 함
        result[0].productId shouldBe testProducts[3].id // 인기 상품 1 (판매량 50)
        result[1].productId shouldBe testProducts[4].id // 인기 상품 2 (판매량 40)
        result[2].productId shouldBe testProducts[0].id // 테스트 상품 1 (판매량 30)
    }
}
