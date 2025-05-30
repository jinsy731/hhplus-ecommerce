package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.CouponEvent
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.IssuedStatus
import kr.hhplus.be.server.shared.time.ClockHolder
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponIssueConsumerService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val couponKVStore: CouponKVStore,
    private val clockHolder: ClockHolder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 쿠폰 발급 이벤트 배치 처리
     * Redis에서 이미 검증되었으므로 DB 저장에만 집중
     */
    @Transactional
    fun processCouponIssueRequestsBatch(events: List<CouponEvent.IssueRequested>) {
        if (events.isEmpty()) return
        
        logger.info("쿠폰 발급 배치 처리 시작: 크기={}", events.size)
        
        try {
            // 쿠폰별 그룹핑하여 처리
            val groupedByCoupon = events.groupBy { it.payload.couponId }
            
            groupedByCoupon.forEach { (couponId, eventList) ->
                processEventsByCoupon(couponId, eventList)
            }
            
            logger.info("쿠폰 발급 배치 처리 완료")
            
        } catch (e: Exception) {
            logger.error("쿠폰 발급 배치 처리 실패", e)
            throw e
        }
    }
    
    /**
     * 단일 쿠폰에 대한 이벤트들을 배치 처리
     */
    private fun processEventsByCoupon(couponId: Long, events: List<CouponEvent.IssueRequested>) {
        try {
            val coupon = couponRepository.getById(couponId)
            
            val userCoupons = events.map { event ->
                coupon.asyncIssueTo(event.payload.userId, event.payload.issuedAt)
            }
            
            val savedUserCoupons = userCouponRepository.saveAll(userCoupons)
            logger.info("쿠폰 ID {} 배치 저장 완료: {}개", couponId, savedUserCoupons.size)
            
            // 각 이벤트에 대해 발급 상태를 ISSUED로 업데이트
            events.forEach { event ->
                couponKVStore.setIssuedStatus(event.payload.userId, event.payload.couponId, IssuedStatus.ISSUED)
                logger.debug("발급 상태 ISSUED로 업데이트: userId={}, couponId={}", 
                    event.payload.userId, event.payload.couponId)
            }
            
        } catch (e: DataIntegrityViolationException) {
            // 중복 제약 조건 위반 시 개별 처리로 fallback
            logger.warn("배치 저장 실패, 개별 처리로 전환: couponId={}", couponId)
            events.forEach { event -> processSingleCouponIssueRequest(event) }
        } catch (e: Exception) {
            logger.error("쿠폰 배치 처리 실패: couponId={}", couponId, e)
            throw e
        }
    }

    /**
     * 단일 쿠폰 발급 이벤트 처리
     * Redis에서 이미 검증되었으므로 DB 저장만 수행
     */
    @Transactional
    fun processSingleCouponIssueRequest(event: CouponEvent.IssueRequested) {
        logger.info("쿠폰 발급 처리 시작: userId={}, couponId={}", 
            event.payload.userId, event.payload.couponId)
        
        try {
            val coupon = couponRepository.getById(event.payload.couponId)
            val userCoupon = coupon.asyncIssueTo(event.payload.userId, event.payload.issuedAt)
            val savedUserCoupon = userCouponRepository.save(userCoupon)
            
            logger.info("쿠폰 발급 처리 완료: userCouponId={}", savedUserCoupon.id)
            
            // 발급 상태를 ISSUED로 업데이트
            couponKVStore.setIssuedStatus(event.payload.userId, event.payload.couponId, IssuedStatus.ISSUED)
            logger.debug("발급 상태 ISSUED로 업데이트: userId={}, couponId={}", 
                event.payload.userId, event.payload.couponId)
            
        } catch (e: DataIntegrityViolationException) {
            // 중복 제약 조건 위반 시 로그만 남기고 무시 (멱등성 보장)
            logger.warn("중복 데이터 무시: userId={}, couponId={}", 
                event.payload.userId, event.payload.couponId)
        } catch (e: Exception) {
            logger.error("쿠폰 발급 처리 실패: userId={}, couponId={}", 
                event.payload.userId, event.payload.couponId, e)
            throw e
        }
    }
} 