package kr.hhplus.be.server.coupon.infrastructure.persistence

import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class DefaultUserCouponRepository(private val jpaRepository: JpaUserCouponRepository): UserCouponRepository {
    override fun findAllByUserIdAndIdIsIn(
        userId: Long,
        ids: List<Long>
    ): List<UserCoupon> {
        return jpaRepository.findAllByUserIdAndIdIsIn(userId, ids)
    }

    override fun findByUserIdAndCouponId(
        userId: Long,
        couponId: Long
    ): UserCoupon? {
        return jpaRepository.findByUserIdAndCouponId(userId, couponId)
    }

    override fun save(userCoupon: UserCoupon): UserCoupon {
        return jpaRepository.save(userCoupon)
    }

    override fun findAllByUserId(
        userId: Long,
        pageable: Pageable
    ): Page<UserCoupon> {
        return jpaRepository.findAllByUserId(userId, pageable)
    }

    override fun saveAll(userCoupons: List<UserCoupon>): List<UserCoupon> {
        return jpaRepository.saveAll(userCoupons)
    }

    override fun findById(id: Long): UserCoupon? {
        return jpaRepository.findById(id).getOrNull()
    }
}