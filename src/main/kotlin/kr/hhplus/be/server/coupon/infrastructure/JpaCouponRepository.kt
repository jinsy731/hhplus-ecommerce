package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.Coupon
import org.springframework.data.jpa.repository.JpaRepository

interface JpaCouponRepository: JpaRepository<Coupon, Long> {
}