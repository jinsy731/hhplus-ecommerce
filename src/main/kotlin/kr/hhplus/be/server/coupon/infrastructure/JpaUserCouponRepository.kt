package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserCouponRepository: JpaRepository<UserCoupon, Long> {
    fun findAllByUserIdAndIdIsIn(userId: Long, ids: List<Long>): List<UserCoupon>
}