package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.RedisCleaner
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.infrastructure.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
class CouponIssueBatchServiceTestIT @Autowired constructor(
    private val couponJpaRepository: JpaCouponRepository,
    private val couponIssueBatchService: CouponIssueBatchService,
    private val redisCleaner: RedisCleaner,
    private val couponKVStore: CouponKVStore,
    private val userCouponJpaRepository: JpaUserCouponRepository
){
    private val now = LocalDateTime.now()
    private val couponId = 99999L
    private val userId1 = 100001L
    private val userId2 = 100002L
    private val userId3 = 100003L
    private val userId4 = 100004L
    private val maxIssueLimit = 10
    
    private lateinit var coupon: Coupon
    
    @BeforeEach
    fun setUp() {
        // 테스트용 할인 정책 생성
        val savedCoupon = couponJpaRepository.save(CouponTestFixture.coupon().build())
        coupon = savedCoupon
        
        redisCleaner.clean()
        // 쿠폰 재고 설정
        couponKVStore.setStock(CouponStock(coupon.id!!, maxIssueLimit.toLong()))
    }
    
    @AfterEach
    fun tearDown() {
        redisCleaner.clean()
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 처리_정상적인 쿠폰 발급 요청이 들어오면 정상적으로 쿠폰이 발급되어야 한다`() {
        // given
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        // Redis List를 사용하도록 변경된 코드에 맞게 테스트 수정
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            val userCoupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            userCoupons.content.size shouldBe 1
            userCoupons.content[0].status shouldBe UserCouponStatus.UNUSED
            
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.ISSUED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
            
            couponKVStore.countIssuedUser(coupon.id!!) shouldBe 2
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 처리_이미 발급된 사용자가 다시 요청하면 중복 발급은 처리되지 않고 새로운 발급만 성공해야 한다`() {
        // given
        couponKVStore.markAsIssued(userId1, coupon.id!!)
        
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
            
            val userCoupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            userCoupons.content.size shouldBe 1
            
            couponKVStore.countIssuedUser(coupon.id!!) shouldBe 2
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 처리_재고가 부족한 상황에서 요청이 들어오면 OutOfStockCouponIdList에 추가되어야 한다`() {
        // given
        couponKVStore.setStock(CouponStock(coupon.id!!, 0))
        
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2),
            CouponIssueRequest(coupon.id!!, userId3)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            // 재고 부족으로 OutOfStockCouponIdList에 추가되었는지 확인
            val outOfStockCouponId = couponKVStore.popFromOutOfStockCouponIdList()
            outOfStockCouponId shouldBe coupon.id
            
            // 실제 발급이 이루어지지 않아야 함
            couponKVStore.countIssuedUser(coupon.id!!) shouldBe 0
            
            // DB에 쿠폰이 생성되지 않았는지 확인
            val userCoupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            userCoupons.content.size shouldBe 0
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 처리_큐에서 꺼내온 요청들이 PROCESSING 상태로 설정된다`() {
        // given
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            // 발급이 성공했는지 확인
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            // 최종 상태가 ISSUED인지 확인
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.ISSUED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 처리_중복 검증 실패 시 FAILED 상태로 설정된다`() {
        // given
        // 이미 발급된 상태로 설정
        couponKVStore.setIssuedUser(userId1, coupon.id!!)

        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
            
            // DB에 실제로 저장된 쿠폰 확인
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            user2Coupons.content.size shouldBe 1
        }
    }
    
    @Test
    @Transactional
    fun `✅재고 부족 시 처리되지 못한 요청들을 배치로 처리하여 FAILED 상태로 설정한다`() {
        // given
        // 재고를 0으로 설정
        couponKVStore.setStock(CouponStock(coupon.id!!, 0))
        
        // 요청 생성 및 큐에 추가
        val batchRequests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2),
            CouponIssueRequest(coupon.id!!, userId3)
        )
        
        batchRequests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        // OutOfStockCouponIdList에 쿠폰 ID 추가
        couponKVStore.pushToOutOfStockCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processOutOfStockRequests()
        
        // then
        assertSoftly {
            // 요청들이 FAILED 상태로 설정되었는지 확인
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId3, coupon.id!!) shouldBe IssuedStatus.FAILED
        }
    }
    
    @Test
    @Transactional
    fun `✅배치 크기보다 많은 요청이 있을 경우 OutOfStockCouponIdList에 다시 추가되어야 한다`() {
        // given
        val batchSize = 2L
        
        // 재고를 0으로 설정
        couponKVStore.setStock(CouponStock(coupon.id!!, 0))
        
        // 배치 크기보다 많은 요청 추가
        val allRequests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2),
            CouponIssueRequest(coupon.id!!, userId3)
        )
        
        allRequests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        // OutOfStockCouponIdList에 쿠폰 ID 추가
        couponKVStore.pushToOutOfStockCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processOutOfStockRequests(batchSize)
        
        // then
        assertSoftly {
            // 처리된 요청들이 FAILED 상태로 설정되었는지 확인
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.FAILED
            
            // 처리되지 않은 요청이 있으므로 아직 처리되지 않았어야 함
            val outOfStockCouponId = couponKVStore.popFromOutOfStockCouponIdList()
            outOfStockCouponId shouldBe coupon.id
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 발급 실패 처리_실패한 요청들을 재처리하여 정상적으로 쿠폰이 발급되어야 한다`() {
        // given
        val failedRequests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        failedRequests.forEach { request ->
            couponKVStore.pushToFailedIssueRequestQueue(request)
        }
        
        couponKVStore.pushToFailedIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processFailedIssueRequest()
        
        // then
        assertSoftly {
            // 발급이 성공했는지 확인
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            // 최종 상태가 ISSUED인지 확인
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.ISSUED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
            
            // DB에 실제로 저장된 쿠폰 확인
            val user1Coupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            
            user1Coupons.content.size shouldBe 1
            user2Coupons.content.size shouldBe 1
        }
    }
    
    @Test
    @Transactional
    fun `✅쿠폰 ID가 없으면 아무 처리도 하지 않고 리턴한다`() {
        // given
        // 처리할 쿠폰 ID가 없는 상황 (처리할 쿠폰 ID 리스트가 비어있는 상태)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        // DB에 저장된 쿠폰이 없어야 함
        val user1Coupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
        user1Coupons.content.size shouldBe 0
    }
    
    @Test
    @Transactional
    fun `✅재고가 정확히 요청 수와 같을 때 모든 요청이 처리된다`() {
        // given
        // 재고를 요청 수와 정확히 같게 설정
        val requestCount = 3
        couponKVStore.setStock(CouponStock(coupon.id!!, requestCount.toLong()))
        
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2),
            CouponIssueRequest(coupon.id!!, userId3)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            // 모든 요청이 처리되었는지 확인
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId3, coupon.id!!) shouldBe true
            
            // DB에 실제로 저장된 쿠폰 확인
            val user1Coupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            val user3Coupons = userCouponJpaRepository.findAllByUserId(userId3, PageRequest.of(0, 10))
            
            user1Coupons.content.size shouldBe 1
            user2Coupons.content.size shouldBe 1
            user3Coupons.content.size shouldBe 1
        }
    }
    
    @Test
    @Transactional
    fun `✅요청 큐에서 요청이 없으면 처리를 종료한다`() {
        // given
        // 쿠폰 ID는 있지만 요청 큐에 요청이 없는 상황
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        // DB에 저장된 쿠폰이 없어야 함
        val userCoupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
        userCoupons.content.size shouldBe 0
    }
    
    @Test
    @Transactional
    fun `✅일부 요청만 처리 가능한 경우 남은 요청은 OutOfStockCouponIdList에 추가되어야 한다`() {
        // given
        // 재고를 2개로 설정
        couponKVStore.setStock(CouponStock(coupon.id!!, 2))
        
        val requests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2),
            CouponIssueRequest(coupon.id!!, userId3),
            CouponIssueRequest(coupon.id!!, userId4)
        )
        
        requests.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processIssueRequest()
        
        // then
        assertSoftly {
            // 재고 만큼만 발급되었는지 확인
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            // OutOfStockCouponIdList에 추가되었는지 확인
            val outOfStockCouponId = couponKVStore.popFromOutOfStockCouponIdList()
            outOfStockCouponId shouldBe coupon.id
            
            // DB에 저장된 쿠폰 확인
            val user1Coupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            
            user1Coupons.content.size shouldBe 1
            user2Coupons.content.size shouldBe 1
        }
    }
    
    @Test
    @Transactional
    fun `✅여러 개의 쿠폰을 순차적으로 처리할 수 있다`() {
        // given
        // 두 번째 쿠폰 생성
        val savedCoupon2 = couponJpaRepository.save(CouponTestFixture.coupon().build())
        val couponId2 = savedCoupon2.id!!
        
        // 두 쿠폰 모두 재고 설정
        couponKVStore.setStock(CouponStock(coupon.id!!, 5))
        couponKVStore.setStock(CouponStock(couponId2, 5))
        
        // 첫 번째 쿠폰 요청 설정
        val requests1 = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        requests1.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        // 두 번째 쿠폰 요청 설정
        val requests2 = listOf(
            CouponIssueRequest(couponId2, userId1),
            CouponIssueRequest(couponId2, userId2)
        )
        
        requests2.forEach { request ->
            couponKVStore.pushToIssueReqeustQueue(request)
        }
        
        // 처리 대기 리스트에 두 쿠폰 ID 추가
        couponKVStore.pushToIssueRequestedCouponIdList(coupon.id!!)
        couponKVStore.pushToIssueRequestedCouponIdList(couponId2)
        
        // when - 두 번 처리
        couponIssueBatchService.processIssueRequest() // 첫 번째 쿠폰 처리
        couponIssueBatchService.processIssueRequest() // 두 번째 쿠폰 처리
        
        // then
        assertSoftly {
            // 첫 번째 쿠폰 발급 확인
            couponKVStore.existsIssuedUser(userId1, coupon.id!!) shouldBe true
            couponKVStore.existsIssuedUser(userId2, coupon.id!!) shouldBe true
            
            // 두 번째 쿠폰 발급 확인
            couponKVStore.existsIssuedUser(userId1, couponId2) shouldBe true
            couponKVStore.existsIssuedUser(userId2, couponId2) shouldBe true
            
            // DB에 실제로 저장된 쿠폰 확인
            val user1Coupons = userCouponJpaRepository.findAllByUserId(userId1, PageRequest.of(0, 10))
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            
            // 각 사용자당 2개의 쿠폰이 발급되어야 함
            user1Coupons.content.size shouldBe 2
            user2Coupons.content.size shouldBe 2
        }
    }

    
    @Test
    @Transactional
    fun `✅실패한 요청 처리 시 중복 검증 실패 시 FAILED 상태로 설정된다`() {
        // given
        // 이미 발급된 상태로 설정
        couponKVStore.setIssuedUser(userId1, coupon.id!!)

        val failedRequests = listOf(
            CouponIssueRequest(coupon.id!!, userId1),
            CouponIssueRequest(coupon.id!!, userId2)
        )
        
        failedRequests.forEach { request ->
            couponKVStore.pushToFailedIssueRequestQueue(request)
        }
        
        couponKVStore.pushToFailedIssueRequestedCouponIdList(coupon.id!!)
        
        // when
        couponIssueBatchService.processFailedIssueRequest()
        
        // then
        assertSoftly {
            couponKVStore.getIssuedStatus(userId1, coupon.id!!) shouldBe IssuedStatus.FAILED
            couponKVStore.getIssuedStatus(userId2, coupon.id!!) shouldBe IssuedStatus.ISSUED
            
            // DB에 실제로 저장된 쿠폰 확인
            val user2Coupons = userCouponJpaRepository.findAllByUserId(userId2, PageRequest.of(0, 10))
            user2Coupons.content.size shouldBe 1
        }
    }
}
