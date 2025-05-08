package kr.hhplus.be.server.order.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.executeConcurrently
import kr.hhplus.be.server.order.domain.OrderRepository
import kr.hhplus.be.server.order.facade.OrderCriteria
import kr.hhplus.be.server.order.facade.OrderFacade
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.infrastructure.ProductVariantJpaRepository
import kr.hhplus.be.server.user.UserPointTestFixture
import kr.hhplus.be.server.user.infrastructure.JpaUserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderFacadeConcurrencyTestIT @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val jpaUserPointRepository: JpaUserPointRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val productVariantJpaRepository: ProductVariantJpaRepository,
    private val databaseCleaner: MySqlDatabaseCleaner,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @AfterEach
    fun tearDown() = databaseCleaner.clean()

    @Test
    fun `✅주문 동시성 테스트_동일 상품, 동일 옵션에 대한 120개의 동시 주문 요청에도 재고가 정확히 감소해야 하고 재고가 부족한 경우 실패해야 한다`() {
        // arrange: 재고가 100개인 옵션을 가진 상품
        val userPoints = IntRange(1, 120).map {
            logger.info { "UserPoint userId=$it" }
            UserPointTestFixture.userPoint(userId = it.toLong(), balance = Money.Companion.of(1500)).build()
        }
        val product = ProductTestFixture.product()
            .withVariants(ProductTestFixture.variant(stock = 100))
            .build()

        jpaUserPointRepository.saveAll(userPoints)
        val savedProduct = productRepository.save(product)
        val variant = productVariantJpaRepository.findByProductId(savedProduct.id!!)!!

        // act: 동일상품, 옵션에 대한 120개의 동시 주문 요청
        executeConcurrently(120) {
            logger.info { "cycle#$it :: userId=${it + 1}" }
            val cri = OrderCriteria.PlaceOrder.Root(
                userId = (it + 1).toLong(),
                items = listOf(
                    OrderCriteria.PlaceOrder.Item(
                        productId = savedProduct.id!!,
                        variantId = variant.id!!,
                        quantity = 1
                    )
                ),
                userCouponIds = listOf(),
            )
            orderFacade.placeOrder(cri)
        }

        // assert: 주문은 정확히 100개만 생성, 100명의 유저는 잔액이 0이고 20명의 유저는 잔액이 1500이어야 한다. 재고는 0이어야 한다.
        val findProduct = productRepository.getById(savedProduct.id!!)
        val findVariant = productVariantJpaRepository.findByProductId(findProduct.id!!)!!
        val findUserPoint = jpaUserPointRepository.findAll()
        val orders = orderRepository.findAll()

        findVariant.stock shouldBe 0
        findUserPoint.filter { it.balance == Money.Companion.ZERO } shouldHaveSize 100
        findUserPoint.filter { it.balance == Money.Companion.of(1500) } shouldHaveSize 20
        orders shouldHaveSize 100
    }
}