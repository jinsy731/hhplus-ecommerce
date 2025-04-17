package kr.hhplus.be.server.product.application

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.common.exception.ProductUnavailableException
import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.common.exception.VariantOutOfStockException
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.jvm.optionals.getOrNull

@SpringBootTestWithMySQLContainer
class ProductServiceTestIT @Autowired constructor(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
    private val productVariantRepository: ProductVariantJpaRepository
) {
    private val testProducts = mutableListOf<Product>()

    @BeforeEach
    fun setup() {
        // 테스트 데이터 준비
        val product1 = Product(
            name = "테스트 상품 1",
            basePrice = BigDecimal(10000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = BigDecimal(1000),
                    stock = 10
                )
            )
            addVariant(
                ProductVariant(
                    additionalPrice = BigDecimal(2000),
                    stock = 5
                )
            )
        }

        val product2 = Product(
            name = "테스트 상품 2",
            basePrice = BigDecimal(20000),
            status = ProductStatus.ON_SALE
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = BigDecimal(0),
                    stock = 8
                )
            )
        }

        val product3 = Product(
            name = "품절 상품",
            basePrice = BigDecimal(15000),
            status = ProductStatus.OUT_OF_STOCK
        ).apply {
            addVariant(
                ProductVariant(
                    additionalPrice = BigDecimal(500),
                    stock = 0
                )
            )
        }

        testProducts.addAll(listOf(
            productRepository.save(product1),
            productRepository.save(product2),
            productRepository.save(product3)
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
        val cmd = ProductCommand.RetrieveList(pageable, "테스트")

        // Act
        val result = productService.retrieveList(cmd)

        // Assert
        result.products shouldHaveSize 2
        result.products.map { it.name } shouldContainExactlyInAnyOrder listOf("테스트 상품 1", "테스트 상품 2")
        result.pageResult.totalElements shouldBe 2
    }

    @Test
    fun `✅ID 목록으로 상품들을 조회할 수 있다`() {
        // Arrange
        val ids = testProducts.map { it.id!! }

        // Act
        val result = productService.findAllById(ids)

        // Assert
        result shouldHaveSize 3
        result.map { it.id } shouldContainExactlyInAnyOrder ids
    }

    @Test
    fun `✅구매 가능한 상품과 옵션은 검증을 통과한다`() {
        // Arrange
        val product = testProducts[0]
        val variantId = product.variants[0].id
        val cmd = ProductCommand.ValidatePurchasability.Root(
            listOf(
                ProductCommand.ValidatePurchasability.Item(
                    productId = product.id!!,
                    variantId = variantId!!,
                    quantity = 5
                )
            )
        )

        // Act & Assert
        productService.validatePurchasability(cmd) // 예외가 발생하지 않아야 함
    }

    @Test
    fun `❌품절된 상품(status=OUT_OF_STOCK)은 구매 불가능하다`() {
        // Arrange
        val outOfStockProduct = testProducts[2]
        val variantId = outOfStockProduct.variants[0].id
        val cmd = ProductCommand.ValidatePurchasability.Root(
            listOf(
                ProductCommand.ValidatePurchasability.Item(
                    productId = outOfStockProduct.id!!,
                    variantId = variantId!!,
                    quantity = 1
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<ProductUnavailableException> {
            productService.validatePurchasability(cmd)
        }
    }

    @Test
    fun `❌존재하지 않는 상품은 구매 검증 시 예외가 발생한다`() {
        // Arrange
        val nonExistentProductId = 9999L
        val cmd = ProductCommand.ValidatePurchasability.Root(
            listOf(
                ProductCommand.ValidatePurchasability.Item(
                    productId = nonExistentProductId,
                    variantId = 1L,
                    quantity = 1
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<ResourceNotFoundException> {
            productService.validatePurchasability(cmd)
        }
    }

    @Test
    fun `❌주문 수량이 재고보다 많으면 예외가 발생한다`() {
        // Arrange
        val product = testProducts[0]
        val variant = product.variants[1] // 재고 5개
        val cmd = ProductCommand.ValidatePurchasability.Root(
            listOf(
                ProductCommand.ValidatePurchasability.Item(
                    productId = product.id!!,
                    variantId = variant.id!!,
                    quantity = 10 // 재고보다 많은 수량
                )
            )
        )

        // Act & Assert
        shouldThrowExactly<VariantOutOfStockException> {
            productService.validatePurchasability(cmd)
        }
    }

    @Test
    fun `✅구매로 재고를 감소시킬 수 있다`() {
        // Arrange
        val product = testProducts[0]
        val variant = product.variants[0]
        val initialStock = variant.stock
        val quantity = 3
        val cmd = ProductCommand.ReduceStockByPurchase.Root(
            listOf(
                ProductCommand.ReduceStockByPurchase.Item(
                    productId = product.id!!,
                    variantId = variant.id!!,
                    quantity = quantity
                )
            )
        )

        // Act
        productService.reduceStockByPurchase(cmd)

        // Assert
        val updatedVariant = productVariantRepository.findById(variant.id!!).getOrNull() ?: throw IllegalStateException()
        updatedVariant.stock shouldBe (initialStock - quantity)
    }
}
