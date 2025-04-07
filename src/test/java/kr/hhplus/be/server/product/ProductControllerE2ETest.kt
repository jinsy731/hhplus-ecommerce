package kr.hhplus.be.server.product

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.CommonResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ProductControllerE2ETest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `상품 목록 조회 - 성공`() {
        val response = restTemplate.exchange(
            "/api/v1/products?page=0&size=20",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<ProductListResponse>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.code shouldBe "SUCCESS"
        response.body?.data.shouldNotBeNull()
        response.body?.data?.products.shouldNotBeEmpty()
        response.body?.data?.pageInfo.shouldNotBeNull()
    }


    @Test
    fun `인기 상품 조회 - 성공`() {
        val response = restTemplate.exchange(
            "/api/v1/products/popular",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<List<PopularProductResponse>>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.code shouldBe "SUCCESS"
        response.body?.data.shouldNotBeNull().shouldNotBeEmpty()
    }
}
