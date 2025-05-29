package kr.hhplus.be.server.product.application

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.application.mapper.ProductMapper
import kr.hhplus.be.server.product.domain.product.model.Product
import kr.hhplus.be.server.product.domain.product.model.ProductRepository
import kr.hhplus.be.server.product.domain.product.model.ProductStatus
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

class ProductServiceTest {
    private lateinit var productService: ProductService
    private lateinit var productRepository: ProductRepository
    private lateinit var popularProductsDailyRepository: JpaPopularProductsDailyRepository
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var testProducts: List<Product>
    private val productMapper = ProductMapper()

    @BeforeEach
    fun setup() {
        productRepository = mockk()
        eventPublisher = mockk()
        popularProductsDailyRepository = mockk()
        productService = ProductService(
            productRepository = productRepository,
            popularProductsDailyRepository = popularProductsDailyRepository,
            eventPublisher = eventPublisher,
            productMapper = productMapper
        )

        testProducts = listOf(
            ProductTestFixture.createValidProduct(1L, variantIds = listOf(1,2)),
            ProductTestFixture.createValidProduct(2L, variantIds = listOf(3,4)))
    }

    @AfterEach
    fun tearDown() {
        clearMocks(productRepository)
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
    fun `✅재고 복구가 정상적으로 동작한다`() {
        // arrange
        val orderId = 1L
        val cmd = ProductCommand.RestoreStock.Root(
            items = listOf(
                ProductCommand.RestoreStock.Item(
                    productId = 1L,
                    variantId = 1L,
                    quantity = 5
                )
            ),
            orderId = orderId,
        )
        every { productRepository.findAllByIdForUpdate(any()) } returns listOf(testProducts[0])
        every { productRepository.findAllVariantsByIdForUpdate(any()) } returns testProducts[0].variants

        // act
        shouldNotThrowAny { productService.restoreStock(cmd) }

        // assert
        testProducts[0].variants[0].stock shouldBe 15
    }

    @Test
    fun `❌재고 복구 시 잘못된 상품 ID가 주어지면 예외가 발생한다`() {
        // arrange
        val cmd = ProductCommand.RestoreStock.Root(
            items = listOf(
                ProductCommand.RestoreStock.Item(
                    productId = 999L,
                    variantId = 1L,
                    quantity = 5
                )
            ),
            orderId = 1L
        )
        every { productRepository.findAllByIdForUpdate(any()) } returns emptyList()
        every { productRepository.findAllVariantsByIdForUpdate(any()) } returns emptyList()

        // act, assert
        shouldThrowExactly<ResourceNotFoundException> { productService.restoreStock(cmd) }
    }
}