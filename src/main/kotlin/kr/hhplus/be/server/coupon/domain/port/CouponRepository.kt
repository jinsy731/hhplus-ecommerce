package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository{
    fun getById(id: Long): Coupon
}

interface UserCouponRepository{
    fun findAllByUserIdAndIdIsIn(userId: Long, ids: List<Long>): List<UserCoupon>
    fun save(userCoupon: UserCoupon): UserCoupon
    fun saveAll(userCoupons: List<UserCoupon>): List<UserCoupon>
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<UserCoupon>
}
