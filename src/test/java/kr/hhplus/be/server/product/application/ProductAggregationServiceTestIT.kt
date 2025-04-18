package kr.hhplus.be.server.product.application

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.SpringBootTestWithMySQLContainer
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpoint
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpointRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesLog
import kr.hhplus.be.server.product.domain.stats.ProductSalesLogRepository
import kr.hhplus.be.server.product.domain.stats.TransactionType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTestWithMySQLContainer
class ProductAggregationServiceTestIT {

    @Autowired
    lateinit var productAggregationService: ProductAggregationService

    @Autowired
    lateinit var productSalesLogRepository: ProductSalesLogRepository

    @Autowired
    lateinit var productSalesAggregationDailyRepository: ProductSalesAggregationDailyRepository

    @Autowired
    lateinit var productSalesAggregationDailyCheckpointRepository: ProductSalesAggregationDailyCheckpointRepository

    private val today = LocalDate.now()
    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        // 테스트 데이터 준비
        // 상품 1 판매 로그
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 10,
                type = TransactionType.SOLD,
                timestamp = now
            )
        )

        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 2L,
                productId = 1L,
                variantId = 1L,
                quantity = 2,
                type = TransactionType.RETURN,
                timestamp = now
            )
        )

        // 상품 2 판매 로그
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 3L,
                productId = 2L,
                variantId = 1L,
                quantity = 5,
                type = TransactionType.SOLD,
                timestamp = now
            )
        )
    }

    @Test
    @Transactional
    fun `판매 로그를 집계하여 일별 통계를 생성한다`() {
        // Arrange
        val batchSize = 10L

        // Act
        productAggregationService.aggregateSinceLastSummary(batchSize, today)

        // Assert
        val product1Daily = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(1L, today))
        product1Daily.shouldNotBeNull()
        product1Daily.salesCount shouldBe 8L // 10(판매) - 2(반품) = 8

        val product2Daily = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(2L, today))
        product2Daily.shouldNotBeNull()
        product2Daily.salesCount shouldBe 5L

        val checkpoint = productSalesAggregationDailyCheckpointRepository.findLast()
        checkpoint.shouldNotBeNull()
        checkpoint.lastAggregatedLogId shouldBeGreaterThanOrEqual 1L
    }

    @Test
    @Transactional
    fun `판매와 반품을 처리하여 일별 판매량을 올바르게 계산한다`() {
        // Arrange
        val batchSize = 10L
        
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 4L,
                productId = 3L,
                variantId = 1L,
                quantity = 3,
                type = TransactionType.SOLD,
                timestamp = now
            )
        )
        
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 5L,
                productId = 3L,
                variantId = 1L,
                quantity = 5,
                type = TransactionType.RETURN,
                timestamp = now
            )
        )

        // Act
        productAggregationService.aggregateSinceLastSummary(batchSize, today)

        // Assert
        val product3Daily = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(3L, today))
        product3Daily.shouldNotBeNull()
        product3Daily.salesCount shouldBe -2L // 3(판매) - 5(반품) = -2
    }

    @Test
    @Transactional
    fun `여러 번 집계를 실행해도 누적 판매량이 올바르게 계산된다`() {
        // Arrange
        val batchSize = 10L
        
        // 첫 번째 집계 실행
        productAggregationService.aggregateSinceLastSummary(batchSize, today)
        
        // 추가 판매 로그 생성
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 6L,
                productId = 1L,
                variantId = 1L,
                quantity = 7,
                type = TransactionType.SOLD,
                timestamp = now
            )
        )

        // Act
        productAggregationService.aggregateSinceLastSummary(batchSize, today)

        // Assert
        val product1Daily = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(1L, today))
        product1Daily.shouldNotBeNull()
        product1Daily.salesCount shouldBe 15L // 8(이전 집계) + 7(새로운 판매) = 15
    }

    @Test
    @Transactional
    fun `체크포인트가 없는 경우에도 올바르게 집계된다`() {
        // Arrange
        val batchSize = 5L
        
        val checkpoint = productSalesAggregationDailyCheckpointRepository.findLast()
        if (checkpoint != null) {
            productSalesAggregationDailyCheckpointRepository.save(
                ProductSalesAggregationDailyCheckpoint(
                    id = checkpoint.id,
                    lastAggregatedLogId = 0L
                )
            )
        }
        
        // Act
        productAggregationService.aggregateSinceLastSummary(batchSize, today)
        
        // Assert
        val newCheckpoint = productSalesAggregationDailyCheckpointRepository.findLast()
        newCheckpoint.shouldNotBeNull()
        newCheckpoint.lastAggregatedLogId shouldBeGreaterThanOrEqual 1L
        
        val product1Daily = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(1L, today))
        product1Daily.shouldNotBeNull()
    }
    
    @Test
    @Transactional
    fun `기존 집계 데이터가 없는 경우 새로운 집계 데이터를 생성한다`() {
        // Arrange
        val testDate = LocalDate.of(2025, 6, 1)
        val batchSize = 10L
        
        val newProductId = 999L
        productSalesLogRepository.save(
            ProductSalesLog(
                orderId = 100L,
                productId = newProductId,
                variantId = 1L,
                quantity = 15,
                type = TransactionType.SOLD,
                timestamp = now
            )
        )
        
        val existingAggregation = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(newProductId, testDate))
        existingAggregation.shouldBeNull()
        
        // Act
        productAggregationService.aggregateSinceLastSummary(batchSize, testDate)
        
        // Assert
        val newAggregation = productSalesAggregationDailyRepository.findById(ProductSalesAggregationDailyId(newProductId, testDate))
        newAggregation.shouldNotBeNull()
        newAggregation.salesCount shouldBe 15L
        
        val checkpoint = productSalesAggregationDailyCheckpointRepository.findLast()
        checkpoint.shouldNotBeNull()
        checkpoint.lastAggregatedLogId shouldBeGreaterThanOrEqual 1L
    }
}