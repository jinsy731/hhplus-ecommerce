package kr.hhplus.be.server.user

import io.kotest.matchers.comparables.shouldBeGreaterThan
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
class UserBalanceE2ETest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `잔액 충전 - 성공`() {
        val userId = 1L
        val request = mapOf("amount" to 5000)


        val response = restTemplate.exchange(
            "/api/v1/users/$userId/balance",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<CommonResponse<BalanceResponse>>() {}
        )

        response.statusCode shouldBe
        response.statusCode shouldBe HttpStatus.OK
        response.body?.code shouldBe "SUCCESS"
        response.body?.data.shouldNotBeNull()
        response.body?.data?.userId shouldBe userId
        response.body?.data?.balance?.shouldBeGreaterThan(0)
    }

    @Test
    fun `잔액 조회 - 성공`() {
        val userId = 1L

        val response = restTemplate.exchange(
            "/api/v1/users/$userId/balance",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<BalanceResponse>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.code shouldBe "SUCCESS"
        response.body?.data.shouldNotBeNull()
        response.body?.data?.userId shouldBe userId
    }
}