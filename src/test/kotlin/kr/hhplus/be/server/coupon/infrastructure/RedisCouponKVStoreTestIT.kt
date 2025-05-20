package kr.hhplus.be.server.coupon.infrastructure

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.coupon.application.CouponKeyGenerator
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponIssueRequest
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponStock
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
import kr.hhplus.be.server.coupon.infrastructure.kvstore.RedisCouponKVStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class RedisCouponKVStoreTestIT @Autowired constructor(
    private val redisCouponKVStore: RedisCouponKVStore,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleaner: RedisCleaner
) {

    @BeforeEach
    fun setUp() {
        redisCleaner.clean()
    }

    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }

    @Test
    fun `사용자 쿠폰 발급 여부 세트 관련 기능이 정상 동작한다`() {
        val userId = 1L
        val couponId = 100L
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)

        var isExists = redisCouponKVStore.existsIssuedUser(userId, couponId)
        isExists shouldBe false
        redisCouponKVStore.setIssuedUser(userId, couponId)

        isExists = redisCouponKVStore.existsIssuedUser(userId, couponId)
        isExists shouldBe true
    }

    @Test
    fun `쿠폰 발급 요청을 큐에 정상적으로 추가한다`() {
        val userId = 1L
        val couponId = 100L
        val issueRequest = CouponIssueRequest(couponId, userId)
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)

        redisCouponKVStore.pushToIssueReqeustQueue(issueRequest)

        val size = redisTemplate.opsForZSet().size(key)
        size shouldBe 1
    }

    @Test
    fun `쿠폰 발급 요청을 큐에서 배치로 정상적으로 가져온다`() {
        val userId1 = 1L
        val userId2 = 2L
        val couponId = 100L
        val issueRequest1 = CouponIssueRequest(couponId, userId1)
        val issueRequest2 = CouponIssueRequest(couponId, userId2)

        redisCouponKVStore.pushToIssueReqeustQueue(issueRequest1)
        redisCouponKVStore.pushToIssueReqeustQueue(issueRequest2)
        val result = redisCouponKVStore.popBatchFromIssueRequestQueue(couponId, 2)

        result.size shouldBe 2
        result[0].userId shouldBe userId1
        result[1].userId shouldBe userId2
    }

    @Test
    fun `실패한 쿠폰 발급 요청을 큐에 정상적으로 추가한다`() {
        val userId = 1L
        val couponId = 100L
        val issueRequest = CouponIssueRequest(couponId, userId)
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(couponId)

        redisCouponKVStore.pushToFailedIssueRequestQueue(issueRequest)

        val size = redisTemplate.opsForList().size(key)
        size shouldBe 1
    }

    @Test
    fun `여러 실패한 쿠폰 발급 요청을 큐에 정상적으로 추가한다`() {
        val userId1 = 1L
        val userId2 = 2L
        val couponId = 100L
        val issueRequest1 = CouponIssueRequest(couponId, userId1)
        val issueRequest2 = CouponIssueRequest(couponId, userId2)
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(couponId)

        redisCouponKVStore.pushAllToFailedIssueRequestQueue(listOf(issueRequest1, issueRequest2))

        val size = redisTemplate.opsForList().size(key)
        size shouldBe 2
    }

    @Test
    fun `실패한 쿠폰 발급 요청을 큐에서 배치로 정상적으로 가져온다`() {
        val userId1 = 1L
        val userId2 = 2L
        val couponId = 100L
        val issueRequest1 = CouponIssueRequest(couponId, userId1)
        val issueRequest2 = CouponIssueRequest(couponId, userId2)

        redisCouponKVStore.pushToFailedIssueRequestQueue(issueRequest1)
        redisCouponKVStore.pushToFailedIssueRequestQueue(issueRequest2)
        val result = redisCouponKVStore.popBatchFromFailedIssueRequestQueue(couponId, 2)

        result.size shouldBe 2
        result[0].userId shouldBe userId1
        result[1].userId shouldBe userId2
    }

    @Test
    fun `쿠폰 재고를 정상적으로 저장하고 조회한다`() {
        val couponId = 100L
        val stock = 1000L
        val couponStock = CouponStock(couponId, stock)

        redisCouponKVStore.setStock(couponStock)
        val result = redisCouponKVStore.getStock(couponId)

        result.couponId shouldBe couponId
        result.stock shouldBe stock
    }

    @Test
    fun `여러 쿠폰의 재고를 정상적으로 조회한다`() {
        val couponId = 100L
        val stock = 1000L
        val couponStock = CouponStock(couponId, stock)

        redisCouponKVStore.setStock(couponStock)
        val result = redisCouponKVStore.getStocks(listOf(couponId))

        result.couponId shouldBe couponId
        result.stock shouldBe stock
    }

    @Test
    fun `쿠폰 발급 상태를 정상적으로 저장하고 조회한다`() {
        val userId = 1L
        val couponId = 100L
        val status = IssuedStatus.ISSUED

        redisCouponKVStore.setIssuedStatus(userId, couponId, status)
        val result = redisCouponKVStore.getIssuedStatus(userId, couponId)

        result shouldBe IssuedStatus.ISSUED
    }
} 