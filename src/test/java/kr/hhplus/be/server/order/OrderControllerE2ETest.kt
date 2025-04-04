package kr.hhplus.be.server.order

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.ApiResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class OrderControllerE2ETest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `주문 생성 - 성공`() {
        val request = mapOf(
            "userId" to 1,
            "items" to listOf(
                mapOf("productId" to 1, "variantId" to 101, "quantity" to 2)
            ),
            "userCouponId" to 123
        )

        val response = restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<CreateOrderResponse>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.code shouldBe "SUCCESS"
        response.body?.data.shouldNotBeNull()
        response.body?.data?.status shouldBe "PAID"
        response.body?.data?.finalTotal?.shouldBeGreaterThan(0)
    }
}
