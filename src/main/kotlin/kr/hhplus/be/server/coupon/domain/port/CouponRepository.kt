package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

interface CouponRepository{
    fun findById(id: Long): Coupon?
}

interface UserCouponRepository{
    fun findByUserIdAndId(userId: Long, id: Long): UserCoupon?
    fun getByUserIdAndId(userId: Long, id: Long): UserCoupon
    fun findAllByUserIdAndIdIsIn(userId: Long, ids: List<Long>): List<UserCoupon>
}
