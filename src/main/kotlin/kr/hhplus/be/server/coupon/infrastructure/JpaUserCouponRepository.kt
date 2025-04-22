package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserCouponRepository: JpaRepository<UserCoupon, Long> {
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?
    fun findAllByUserIdAndIdIsIn(userId: Long, ids: List<Long>): List<UserCoupon>
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<UserCoupon>
}