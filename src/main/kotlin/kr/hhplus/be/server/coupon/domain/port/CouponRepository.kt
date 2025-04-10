package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CouponRepository : JpaRepository<Coupon, Long> {
    
    fun findByCode(code: String): Coupon?
    
    fun findByIsActiveTrue(): List<Coupon>
    
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.startAt <= :now AND c.endAt >= :now")
    fun findAllValidCoupons(now: LocalDateTime = LocalDateTime.now()): List<Coupon>
}

@Repository
interface UserCouponRepository : JpaRepository<UserCoupon, Long> {
    
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?
    
    fun findAllByUserId(userId: Long): List<UserCoupon>
    
    fun findAllByUserIdAndIsUsedFalse(userId: Long): List<UserCoupon>
    
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND uc.isUsed = false AND c.isActive = true AND c.startAt <= :now AND c.endAt >= :now")
    fun findAllAvailableUserCoupons(userId: Long, now: LocalDateTime = LocalDateTime.now()): List<UserCoupon>
}
