package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.coupon.domain.model.Coupon
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrElse

@Repository
class DefaultCouponRepository(private val jpaRepository: JpaCouponRepository): CouponRepository {
    override fun getById(id: Long): Coupon {
        return jpaRepository.findById(id).getOrElse { throw ResourceNotFoundException() }
    }

    override fun save(coupon: Coupon): Coupon {
        return jpaRepository.save(coupon)
    }
}