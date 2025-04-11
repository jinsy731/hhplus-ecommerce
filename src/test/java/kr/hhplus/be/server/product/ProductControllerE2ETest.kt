package kr.hhplus.be.server.product

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.product.entrypoint.http.ProductResponse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@Disabled
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
            object : ParameterizedTypeReference<CommonResponse<ProductResponse.Retrieve.Lists>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK

        val data = response.body?.data.shouldNotBeNull()
        val products = data.products.shouldNotBeEmpty()
        val product = products.first()
        
        // 새로운 응답 구조 검증
        product.productId shouldBe 1
        product.name shouldBe "티셔츠"
        product.basePrice shouldBe 29000
        product.status shouldBe "ON_SALE"
        
        // 옵션 스펙 검증
        val optionSpecs = product.optionSpecs.shouldNotBeEmpty()
        optionSpecs.size shouldBe 2
        
        val colorSpec = optionSpecs.first()
        colorSpec.id shouldBe 1
        colorSpec.name shouldBe "색상"
        colorSpec.displayOrder shouldBe 1
        colorSpec.values.shouldNotBeEmpty()
        
        // 상품 옵션 조합 검증
        val variants = product.variants.shouldNotBeEmpty()
        val variant = variants.first()
        variant.variantId shouldBe 101
        variant.optionValueIds.shouldNotBeEmpty()
        variant.additionalPrice shouldBe 1000
        variant.status shouldBe "ACTIVE"
        variant.stock shouldBe 10
        
        // 페이지 정보 검증
        val pageInfo = data.pageInfo.shouldNotBeNull()
        pageInfo.page shouldBe 0
        pageInfo.size shouldBe 20
        pageInfo.totalElement shouldBe 31
        pageInfo.totalPages shouldBe 4
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
