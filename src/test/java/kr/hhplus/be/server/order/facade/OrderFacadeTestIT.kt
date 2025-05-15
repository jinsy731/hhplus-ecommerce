package kr.hhplus.be.server.order.facade

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.order.domain.OrderStatus
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.infrastructure.JpaUserPointRepository
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.rank.infrastructure.persistence.ProductRankingRepository
import kr.hhplus.be.server.shared.domain.Money
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@SpringBootTest
class OrderFacadeTestIT @Autowired constructor(
    private val productJpaRepository: ProductJpaRepository,
    private val userPointJpaRepository: JpaUserPointRepository,
    private val orderFacade: OrderFacade,
    @MockitoSpyBean private val productRankingRepository: ProductRankingRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
) {

    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `✅랭킹 서비스에서 예외가 발생해도 주문은 정상적으로 완료된다`() {
        // given
        val userId = 1L
        val savedProduct = ProductTestFixture
            .product()
            .withVariant(ProductTestFixture.variant())
            .build().let { productJpaRepository.save(it) }
        userPointJpaRepository.save(UserPointTestFixture.userPoint(userId = userId, balance = Money.of(100000)).build())
        val cri = createOrderCriteria(
            productId = savedProduct.id!!,
            variantId = savedProduct.variants.first().id!!,
            userId = userId)

        doThrow(RuntimeException("랭킹 저장 실패")).`when`(productRankingRepository)
            .increaseRanking(any(), any(), any())

        // when
        val order = orderFacade.placeOrder(cri)

        // then
        order.status shouldBe OrderStatus.PAID
        order.userId shouldBe userId
    }

    @Test
    fun `✅주문 완료 시 상품 랭킹이 비동기적으로 업데이트된다`() {
        // given
        val userId = 1L
        val quantity = 5
        val savedProduct = ProductTestFixture
            .product()
            .withVariant(ProductTestFixture.variant())
            .build().let { productJpaRepository.save(it) }
        userPointJpaRepository.save(UserPointTestFixture.userPoint(userId = userId, balance = Money.of(100000)).build())
        val cri = createOrderCriteria(
            productId = savedProduct.id!!,
            variantId = savedProduct.variants.first().id!!,
            userId = userId,
            quantity = quantity
        )

        // when
        orderFacade.placeOrder(cri)

        // then
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted {
                verify(productRankingRepository).increaseRanking(
                    any(),
                    any(),
                    any()
                )
            }
    }

    private fun createOrderCriteria(
        productId: Long,
        variantId: Long,
        userId: Long,
        quantity: Int = 5
    ): OrderCriteria.PlaceOrder.Root {
        val orderItems = listOf(
            OrderCriteria.PlaceOrder.Item(
                productId = productId,
                variantId = variantId,
                quantity = quantity
            )
        )

        return OrderCriteria.PlaceOrder.Root(
            userId = userId,
            items = orderItems,
            timestamp = LocalDateTime.now(),
            userCouponIds = listOf()
        )
    }
} 