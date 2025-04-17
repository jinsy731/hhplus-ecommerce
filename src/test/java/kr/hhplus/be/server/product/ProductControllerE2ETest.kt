package kr.hhplus.be.server.product

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.product.entrypoint.http.ProductResponse
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository
import org.junit.jupiter.api.Disabled
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
    @Autowired
    lateinit var productJpaRepository: ProductJpaRepository

    @Test
    fun `상품 목록 조회 - 성공`() {
        val testProductData = (1..20).map { ProductTestFixture.createValidProduct() }
        productJpaRepository.saveAll(testProductData)

        val response = restTemplate.exchange(
            "/api/v1/products?keyword=테스트&page=0&size=20",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<ProductResponse.Retrieve.Lists>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK

        val data = response.body?.data.shouldNotBeNull()
        val products = data.products.shouldNotBeEmpty()

        // 페이지 정보 검증
        val pageInfo = data.pageInfo.shouldNotBeNull()
        pageInfo.page shouldBe 0
        pageInfo.size shouldBe 20
        pageInfo.totalElement shouldBe 20
        pageInfo.totalPages shouldBe 1
    }


    @Test
    fun `인기 상품 조회 - 성공`() {


        val response = restTemplate.exchange(
            "/api/v1/products/popular",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<List<ProductResponse.Retrieve.Popular>>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull().shouldNotBeEmpty()
    }
}
