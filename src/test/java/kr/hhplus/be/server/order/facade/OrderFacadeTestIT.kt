package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.order.domain.OrderResultSender
import kr.hhplus.be.server.order.infrastructure.persistence.JpaOrderRepository
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.infrastructure.JpaUserPointRepository
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.rank.application.RankingService
import kr.hhplus.be.server.rank.infrastructure.persistence.ProductRankingRepository
import kr.hhplus.be.server.shared.domain.Money
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@SpringBootTest
@RecordApplicationEvents
class OrderFacadeTestIT {

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents // 생성자 주입 안됨

    @Autowired
    private lateinit var orderFacade: OrderFacade

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired
    private lateinit var userPointJpaRepository: JpaUserPointRepository

    @MockitoSpyBean
    private lateinit var productRankingRepository: ProductRankingRepository

    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @MockitoSpyBean
    private lateinit var orderResultSender: OrderResultSender

    @MockitoSpyBean
    private lateinit var rankingService: RankingService

    @Autowired
    private lateinit var orderJpaRepository: JpaOrderRepository


    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `✅주문 완료 후 상품 랭킹이 업데이트된다`() {
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