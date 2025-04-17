package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultUserCouponRepository(private val jpaRepository: JpaUserCouponRepository): UserCouponRepository {
    override fun findAllByUserIdAndIdIsIn(
        userId: Long,
        ids: List<Long>
    ): List<UserCoupon> {
        return jpaRepository.findAllByUserIdAndIdIsIn(userId, ids)
    }

    override fun save(userCoupon: UserCoupon): UserCoupon {
        return jpaRepository.save(userCoupon)
    }

    override fun saveAll(userCoupons: List<UserCoupon>): List<UserCoupon> {
        return jpaRepository.saveAll(userCoupons)
    }
}