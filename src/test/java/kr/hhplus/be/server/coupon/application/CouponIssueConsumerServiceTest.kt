package kr.hhplus.be.server.coupon.application

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import kr.hhplus.be.server.coupon.CouponTestFixture
import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.coupon.domain.CouponIssueRequestedPayload
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.model.UserCouponStatus
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
import kr.hhplus.be.server.shared.time.ClockHolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class CouponIssueConsumerServiceTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var couponKVStore: CouponKVStore
    private lateinit var clockHolder: ClockHolder
    private lateinit var couponIssueConsumerService: CouponIssueConsumerService

    private lateinit var coupon: Coupon
    private lateinit var now: LocalDateTime

    @BeforeEach
    fun setup() {
        couponRepository = mockk()
        userCouponRepository = mockk()
        couponKVStore = mockk()
        clockHolder = mockk()
        couponIssueConsumerService = CouponIssueConsumerService(
            couponRepository = couponRepository,
            userCouponRepository = userCouponRepository,
            couponKVStore = couponKVStore,
            clockHolder = clockHolder
        )
        
        coupon = CouponTestFixture.coupon(id = 1L).build()
        now = LocalDateTime.now()
        every { clockHolder.getNowInLocalDateTime() } returns now
        every { couponKVStore.setIssuedStatus(any(), any(), any()) } just Runs
    }

    @Test
    fun `✅단일 쿠폰 발급 이벤트 처리가 성공한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val issuedAt = now
        
        val event = CouponEvent.IssueRequested(
            payload = CouponIssueRequestedPayload(
                userId = userId,
                couponId = couponId,
                issuedAt = issuedAt
            )
        )

        val userCoupon = UserCoupon(
            id = 1L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.UNUSED,
            issuedAt = issuedAt,
            expiredAt = issuedAt.plusDays(10),
            usedAt = null,
            orderId = null
        )

        every { couponRepository.getById(couponId) } returns coupon
        every { userCouponRepository.save(any<UserCoupon>()) } returns userCoupon

        // when
        couponIssueConsumerService.processSingleCouponIssueRequest(event)

        // then
        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.save(any<UserCoupon>()) }
        verify { couponKVStore.setIssuedStatus(userId, couponId, IssuedStatus.ISSUED) }
    }

    @Test
    fun `✅단일 쿠폰 발급 시 중복 제약 조건 위반이 발생해도 예외가 발생하지 않는다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val issuedAt = now
        
        val event = CouponEvent.IssueRequested(
            payload = CouponIssueRequestedPayload(
                userId = userId,
                couponId = couponId,
                issuedAt = issuedAt
            )
        )

        every { couponRepository.getById(couponId) } returns coupon
        every { userCouponRepository.save(any<UserCoupon>()) } throws 
            DataIntegrityViolationException("중복 제약 조건 위반")

        // when & then - 예외가 발생하지 않아야 함
        couponIssueConsumerService.processSingleCouponIssueRequest(event)

        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.save(any<UserCoupon>()) }
        // 중복 제약 조건 위반 시에는 setIssuedStatus가 호출되지 않아야 함
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
    }

    @Test
    fun `⛔️단일 쿠폰 발급 시 예상치 못한 예외가 발생하면 예외를 전파한다`() {
        // given
        val userId = 1L
        val couponId = 100L
        val issuedAt = now
        
        val event = CouponEvent.IssueRequested(
            payload = CouponIssueRequestedPayload(
                userId = userId,
                couponId = couponId,
                issuedAt = issuedAt
            )
        )

        every { couponRepository.getById(couponId) } returns coupon
        every { userCouponRepository.save(any<UserCoupon>()) } throws 
            RuntimeException("예상치 못한 예외")

        // when & then
        shouldThrow<RuntimeException> {
            couponIssueConsumerService.processSingleCouponIssueRequest(event)
        }

        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.save(any<UserCoupon>()) }
        // 예외 발생 시에는 setIssuedStatus가 호출되지 않아야 함
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
    }

    @Test
    fun `✅배치 쿠폰 발급 이벤트 처리가 성공한다`() {
        // given
        val userIds = listOf(1L, 2L, 3L)
        val couponId = 100L
        val issuedAt = now
        
        val events = userIds.map { userId ->
            CouponEvent.IssueRequested(
                payload = CouponIssueRequestedPayload(
                    userId = userId,
                    couponId = couponId,
                    issuedAt = issuedAt
                )
            )
        }

        val userCoupons = userIds.mapIndexed { index, userId ->
            UserCoupon(
                id = index.toLong() + 1,
                userId = userId,
                couponId = couponId,
                status = UserCouponStatus.UNUSED,
                issuedAt = issuedAt,
                expiredAt = issuedAt.plusDays(10),
                usedAt = null,
                orderId = null
            )
        }

        every { couponRepository.getById(couponId) } returns coupon
        every { userCouponRepository.saveAll(any<List<UserCoupon>>()) } returns userCoupons

        // when
        couponIssueConsumerService.processCouponIssueRequestsBatch(events)

        // then
        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.saveAll(any<List<UserCoupon>>()) }
        // 각 사용자에 대해 setIssuedStatus가 호출되어야 함
        userIds.forEach { userId ->
            verify { couponKVStore.setIssuedStatus(userId, couponId, IssuedStatus.ISSUED) }
        }
    }

    @Test
    fun `✅배치 쿠폰 발급 시 중복 제약 조건 위반 발생하면 개별 처리로 전환한다`() {
        // given
        val userIds = listOf(1L, 2L, 3L)
        val couponId = 100L
        val issuedAt = now
        
        val events = userIds.map { userId ->
            CouponEvent.IssueRequested(
                payload = CouponIssueRequestedPayload(
                    userId = userId,
                    couponId = couponId,
                    issuedAt = issuedAt
                )
            )
        }

        val userCoupon = UserCoupon(
            id = 1L,
            userId = 1L,
            couponId = couponId,
            status = UserCouponStatus.UNUSED,
            issuedAt = issuedAt,
            expiredAt = issuedAt.plusDays(10),
            usedAt = null,
            orderId = null
        )

        every { couponRepository.getById(couponId) } returns coupon
        // 배치 저장 시 실패
        every { userCouponRepository.saveAll(any<List<UserCoupon>>()) } throws 
            DataIntegrityViolationException("중복 제약 조건 위반")
        // 개별 저장: 첫 번째는 성공, 나머지는 중복으로 실패
        every { userCouponRepository.save(any<UserCoupon>()) } returns userCoupon andThenThrows 
            DataIntegrityViolationException("중복") andThenThrows 
            DataIntegrityViolationException("중복")

        // when
        couponIssueConsumerService.processCouponIssueRequestsBatch(events)

        // then
        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.saveAll(any<List<UserCoupon>>()) }
        // 개별 처리로 전환되어 각 이벤트마다 save가 호출됨
        verify(exactly = 3) { userCouponRepository.save(any<UserCoupon>()) }
        // 첫 번째 사용자만 성공하여 setIssuedStatus가 호출됨
        verify(exactly = 1) { couponKVStore.setIssuedStatus(1L, couponId, IssuedStatus.ISSUED) }
        // 나머지 사용자들은 중복으로 실패하여 setIssuedStatus가 호출되지 않음
        verify(exactly = 0) { couponKVStore.setIssuedStatus(2L, couponId, IssuedStatus.ISSUED) }
        verify(exactly = 0) { couponKVStore.setIssuedStatus(3L, couponId, IssuedStatus.ISSUED) }
    }

    @Test
    fun `✅빈 이벤트 리스트 처리 시 아무 작업을 하지 않는다`() {
        // given
        val emptyEvents = emptyList<CouponEvent.IssueRequested>()

        // when
        couponIssueConsumerService.processCouponIssueRequestsBatch(emptyEvents)

        // then
        // 어떤 repository 메서드도 호출되지 않아야 함
        verify(exactly = 0) { couponRepository.getById(any()) }
        verify(exactly = 0) { userCouponRepository.saveAll(any()) }
        verify(exactly = 0) { userCouponRepository.save(any()) }
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
    }

    @Test
    fun `⛔️배치 처리 중 예상치 못한 예외 발생 시 예외를 전파한다`() {
        // given
        val userIds = listOf(1L, 2L)
        val couponId = 100L
        val issuedAt = now
        
        val events = userIds.map { userId ->
            CouponEvent.IssueRequested(
                payload = CouponIssueRequestedPayload(
                    userId = userId,
                    couponId = couponId,
                    issuedAt = issuedAt
                )
            )
        }

        every { couponRepository.getById(couponId) } returns coupon
        every { userCouponRepository.saveAll(any<List<UserCoupon>>()) } throws 
            RuntimeException("예상치 못한 예외")

        // when & then
        shouldThrow<RuntimeException> {
            couponIssueConsumerService.processCouponIssueRequestsBatch(events)
        }

        verify { couponRepository.getById(couponId) }
        verify { userCouponRepository.saveAll(any<List<UserCoupon>>()) }
        // 예외 발생으로 setIssuedStatus가 호출되지 않아야 함
        verify(exactly = 0) { couponKVStore.setIssuedStatus(any(), any(), any()) }
    }
} 