package kr.hhplus.be.server.coupon

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.coupon.application.CouponCommand
import kr.hhplus.be.server.coupon.application.CouponService
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.entrypoint.http.CouponRequest
import kr.hhplus.be.server.coupon.entrypoint.http.CouponResponse
import kr.hhplus.be.server.coupon.infrastructure.JpaCouponRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class UserCouponControllerE2ETest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var couponRepository: JpaCouponRepository

    @Autowired
    private lateinit var couponService: CouponService

    private val testUserId = 1L
    private val testCouponId = 1L // SQL 파일에 정의된 쿠폰 ID 사용

    @Test
    fun `쿠폰 발급 - 성공`() {
        // Given
        val coupon = CouponTestFixture.coupon().build()
        couponRepository.save(coupon)

        val request = CouponRequest.Issue(couponId = testCouponId)

        // When
        val response = restTemplate.exchange(
            "/api/v1/users/$testUserId/coupons",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<CommonResponse<CouponResponse.Issue>>() {}
        )

        // Then
        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.status shouldBe "UNUSED"
    }

    @Test
    fun `보유 쿠폰 목록 조회 - 성공`() {
        // given
        val coupon = CouponTestFixture.coupon().build()
        val savedCoupon = couponRepository.save(coupon)
        couponService.issueCoupon(CouponCommand.Issue(1L, savedCoupon.id!!))

        // When
        val response = restTemplate.exchange(
            "/api/v1/users/$testUserId/coupons?page=0&size=10",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<CouponResponse.RetrieveLists>>() {}
        )

        // Then
        response.statusCode shouldBe HttpStatus.OK
        val responseData = response.body?.data.shouldNotBeNull()
        val coupons = responseData.coupons.shouldNotBeEmpty()
        
        coupons.size shouldBe 1
        
        val pageInfo = responseData.pageInfo
        pageInfo.page shouldBe 0
        pageInfo.size shouldBe 10
        pageInfo.totalElement shouldBe 1
    }
}
