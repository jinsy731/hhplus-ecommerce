package kr.hhplus.be.server.point.entrypoint

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.MySqlDatabaseCleaner
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.point.UserPointTestFixture
import kr.hhplus.be.server.point.entrypoint.http.UserPointResponse
import kr.hhplus.be.server.point.infrastructure.JpaUserPointHistoryRepository
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
class UserPointControllerE2ETest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    @Autowired
    private lateinit var userPointRepository: JpaUserPointRepository
    @Autowired
    private lateinit var userPointHistoryRepository: JpaUserPointHistoryRepository
    @Autowired
    private lateinit var databaseCleaner: MySqlDatabaseCleaner

    @AfterEach
    fun cleanUp() {
        databaseCleaner.clean()
    }

    @Test
    fun `잔액 충전 - 성공`() {
        val userId = 10L
        val userPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = Money.of(1000))
        userPointRepository.save(userPoint)

        val request = mapOf("amount" to 5000)


        val response = restTemplate.exchange(
            "/api/v1/users/$userId/balance",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<CommonResponse<UserPointResponse.Charge>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.userId shouldBe userId
        response.body?.data?.point?.compareTo(BigDecimal(6000)) shouldBe 0
    }

    @Test
    fun `잔액 조회 - 성공`() {
        val userId = 2L
        val userPoint = UserPointTestFixture.createUserPoint(userId = userId, balance = Money.of(1000))
        userPointRepository.save(userPoint)


        val response = restTemplate.exchange(
            "/api/v1/users/$userId/balance",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<CommonResponse<UserPointResponse.Retrieve>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body?.data.shouldNotBeNull()
        response.body?.data?.userId shouldBe userId
        response.body?.data?.point?.compareTo(BigDecimal(1000)) shouldBe 0
    }
}