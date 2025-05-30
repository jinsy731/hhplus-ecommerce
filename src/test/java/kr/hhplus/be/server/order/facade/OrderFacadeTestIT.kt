package kr.hhplus.be.server.order.facade

import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.order.OrderTestFixture
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
import org.mockito.kotlin.times
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
        ProductTestFixture
            .product()
            .withVariant(ProductTestFixture.variant())
            .build().let { productJpaRepository.save(it) }
        userPointJpaRepository.save(UserPointTestFixture.userPoint(userId = userId, balance = Money.of(100000)).build())
        val order = OrderTestFixture.order(userId = userId).withStandardItems().build()
        val savedOrder = orderJpaRepository.save(order)
        val cri = OrderCriteria.ProcessPayment.Root(
            orderId = savedOrder.id!!,
            pgPaymentId = "",
            paymentMethod = "",
            timestamp = LocalDateTime.now(),
        )

        // when
        orderFacade.processPayment(cri)

        // then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                verify(rankingService, times(1)).updateProductRanking(any())
            }
    }
} 