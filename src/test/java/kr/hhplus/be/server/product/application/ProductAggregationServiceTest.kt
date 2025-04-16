package kr.hhplus.be.server.product.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDaily
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpoint
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyCheckpointRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyId
import kr.hhplus.be.server.product.domain.stats.ProductSalesAggregationDailyRepository
import kr.hhplus.be.server.product.domain.stats.ProductSalesLog
import kr.hhplus.be.server.product.domain.stats.ProductSalesLogRepository
import kr.hhplus.be.server.product.domain.stats.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProductAggregationServiceTest {

    private val mockProductSalesLogRepository = mockk<ProductSalesLogRepository>()
    private val mockProductSalesAggregationDailyRepository = mockk<ProductSalesAggregationDailyRepository>()
    private val mockProductSalesAggregationCheckpointRepository = mockk<ProductSalesAggregationDailyCheckpointRepository>()
    private val sut = ProductAggregationService(
        mockProductSalesLogRepository,
        mockProductSalesAggregationDailyRepository,
        mockProductSalesAggregationCheckpointRepository
    )
    
    @Test
    fun `✅배치 집계 성공_집계를 반영하고 새로운 체크포인트를 저장한다`() {
        // arrange
        val day = LocalDate.of(2025, 5, 1)
        val lastCheckpointId = 1L
        val lastCheckpoint = ProductSalesAggregationDailyCheckpoint(lastCheckpointId)
        val newCheckpoint = ProductSalesAggregationDailyCheckpoint(5L)
        val checkpointSlot = slot<ProductSalesAggregationDailyCheckpoint>()
        val product1AggregationSlot = slot<ProductSalesAggregationDaily>()
        val product2AggregationSlot = slot<ProductSalesAggregationDaily>()
        val product1AggregationDaily = ProductSalesAggregationDaily(ProductSalesAggregationDailyId(1L, day), 10)
        val product2AggregationDaily = ProductSalesAggregationDaily(ProductSalesAggregationDailyId(2L, day), 10)
        val productSalesLog = listOf(
            ProductSalesLog(
                id = 5L,
                orderId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 100,
                type = TransactionType.SOLD,
                timestamp = LocalDateTime.now()
            ),
            ProductSalesLog(
                id = 4L,
                orderId = 1L,
                productId = 1L,
                variantId = 1L,
                quantity = 50,
                type = TransactionType.RETURN,
                timestamp = LocalDateTime.now()
            ),
            ProductSalesLog(
                id = 3L,
                orderId = 1L,
                productId = 2L,
                variantId = 1L,
                quantity = 100,
                type = TransactionType.SOLD,
                timestamp = LocalDateTime.now()
            ),
            ProductSalesLog(
                id = 2L,
                orderId = 1L,
                productId = 2L,
                variantId = 1L,
                quantity = 50,
                type = TransactionType.RETURN,
                timestamp = LocalDateTime.now()
            ),
        )

        every { mockProductSalesAggregationCheckpointRepository.findLast() } returns lastCheckpoint
        every { mockProductSalesLogRepository.findForBatch(any(), any()) } returns productSalesLog
        every { mockProductSalesAggregationCheckpointRepository.save(capture(checkpointSlot)) } returns newCheckpoint
        every { mockProductSalesAggregationDailyRepository.findAll(any())} returns listOf(product1AggregationDaily, product2AggregationDaily)
        every { mockProductSalesAggregationDailyRepository.save(capture(product1AggregationSlot)) } returns mockk()
        // act
        sut.aggregateSinceLastSummary(10, day)
        // assert
        verify(exactly = 2) { mockProductSalesAggregationDailyRepository.save(any()) }
        verify(exactly = 1) { mockProductSalesAggregationCheckpointRepository.save(any()) }
        checkpointSlot.captured.lastAggregatedLogId shouldBe 5L
        product1AggregationSlot.captured.salesCount shouldBe 60
    }

    @Test
    fun `❌집계할 로그가 없을 경우 아무 것도 저장하지 않는다`() {
        // arrange
        val today = LocalDate.of(2025, 5, 1)
        every { mockProductSalesAggregationDailyRepository.findAll(any()) } returns emptyList()
        every { mockProductSalesAggregationCheckpointRepository.findLast() } returns null
        every { mockProductSalesLogRepository.findForBatch(any(), any()) } returns emptyList()

        // act
        sut.aggregateSinceLastSummary(10, today)

        // assert
        verify(exactly = 0) { mockProductSalesAggregationDailyRepository.save(any()) }
        verify(exactly = 0) { mockProductSalesAggregationCheckpointRepository.save(any()) }
    }

    @Test
    fun `✅기존 집계가 없는 상품은 새로 생성한다`() {
        // arrange
        val today = LocalDate.of(2025, 5, 1)
        val newLog = ProductSalesLog(
            id = 10L,
            orderId = 1L,
            productId = 99L,
            variantId = 1L,
            quantity = 100,
            type = TransactionType.SOLD,
            timestamp = LocalDateTime.now()
        )

        val checkpointSlot = slot<ProductSalesAggregationDailyCheckpoint>()
        val newAggSlot = slot<ProductSalesAggregationDaily>()

        every { mockProductSalesAggregationCheckpointRepository.findLast() } returns null
        every { mockProductSalesLogRepository.findForBatch(any(), any()) } returns listOf(newLog)
        every { mockProductSalesAggregationDailyRepository.findAll(any()) } returns emptyList()
        every { mockProductSalesAggregationDailyRepository.save(capture(newAggSlot)) } returns mockk()
        every { mockProductSalesAggregationCheckpointRepository.save(capture(checkpointSlot)) } returns mockk()

        // act
        sut.aggregateSinceLastSummary(10, today)

        // assert
        newAggSlot.captured.id.productId shouldBe 99L
        newAggSlot.captured.salesCount shouldBe 100
        checkpointSlot.captured.lastAggregatedLogId shouldBe 10L
    }

    @Test
    fun `🚨판매보다 반품이 많은 경우 음수로 집계된다`() {
        val today = LocalDate.of(2025, 5, 1)
        val logs = listOf(
            ProductSalesLog(5L, 1L, 10L, 1L, 30, TransactionType.SOLD, LocalDateTime.now()),
            ProductSalesLog(4L, 1L, 10L, 1L, 50, TransactionType.RETURN, LocalDateTime.now()),
        )

        val checkpointSlot = slot<ProductSalesAggregationDailyCheckpoint>()
        val savedAggSlot = slot<ProductSalesAggregationDaily>()

        every { mockProductSalesAggregationCheckpointRepository.findLast() } returns null
        every { mockProductSalesLogRepository.findForBatch(any(), any()) } returns logs
        every { mockProductSalesAggregationDailyRepository.findAll(any()) } returns emptyList()
        every { mockProductSalesAggregationDailyRepository.save(capture(savedAggSlot)) } returns mockk()
        every { mockProductSalesAggregationCheckpointRepository.save(capture(checkpointSlot)) } returns mockk()

        // act
        sut.aggregateSinceLastSummary(10, today)

        // assert
        savedAggSlot.captured.salesCount shouldBe -20
    }

}