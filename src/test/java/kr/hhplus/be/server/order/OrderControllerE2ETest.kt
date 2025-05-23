package kr.hhplus.be.server.order

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaUserCouponRepository
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.order.entrypoint.http.OrderResponse
import kr.hhplus.be.server.order.infrastructure.persistence.JpaOrderRepository
import kr.hhplus.be.server.product.ProductTestFixture
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.infrastructure.JpaUserPointRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class OrderControllerE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val databaseCleaner: MySqlDatabaseCleaner,
    private val couponRepository: JpaCouponRepository,
    private val userCouponRepository: JpaUserCouponRepository,
    private val productRepository: ProductJpaRepository,
    private val userPointRepository: JpaUserPointRepository,
    private val orderRepository: JpaOrderRepository
){
    @AfterEach
    fun clean() {
        databaseCleaner.clean()
    }

    @Test
    fun `주문 생성 - 성공`() {
        val userId = 32912L
        val existingUserPoint = userPointRepository.findByUserId(userId)
        existingUserPoint?.let { println("existingUserPoint = $existingUserPoint") }
        val userPoint = userPointRepository.save(UserPointTestFixture.createUserPoint(userId = userId, balance = Money.of(100000)))
        val coupon = couponRepository.save(CouponTestFixture.createValidCoupon())
        val userCoupon = userCouponRepository.save(CouponTestFixture.createUserCoupon(userId = userId, coupon = coupon))
        val product = productRepository.save(ProductTestFixture.createValidProduct())
        val variant = product.variants.first()

        val request = mapOf(
            "userId" to userId,
            "items" to listOf(
                mapOf("productId" to product.id, "variantId" to variant.id, "quantity" to 10)
            ),
            "userCouponIds" to listOf(userCoupon.id),
            "payMethods" to listOf(mapOf(
                "amount" to BigDecimal(15000),
                "method" to "POINT"
            ))
        )

        val response = restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<CommonResponse<OrderResponse.Create>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.status shouldBe OrderStatus.PAID
        response.body?.data?.finalTotal?.compareTo(BigDecimal(10000)) shouldBe 0
    }
}
