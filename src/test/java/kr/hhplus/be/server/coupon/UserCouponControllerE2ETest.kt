package kr.hhplus.be.server.coupon

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.CommonResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class UserCouponControllerE2ETest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `쿠폰 발급 - 성공`() {
        val userId = 1L
        val request = mapOf("couponId" to 12)

        val response = restTemplate.exchange(
            "/api/v1/users/$userId/coupons",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<CommonResponse<kr.hhplus.be.server.coupon.entrypoint.http.IssueCouponResponse>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.status shouldBe "UNUSED"
    }

    @Test
    fun `보유 쿠폰 목록 조회 - 성공`() {
        val userId = 1L

        val response = restTemplate.exchange(
            "/api/v1/users/$userId/coupons",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<kr.hhplus.be.server.coupon.entrypoint.http.UserCouponListResponse>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.coupons.shouldNotBeEmpty()
    }
}
