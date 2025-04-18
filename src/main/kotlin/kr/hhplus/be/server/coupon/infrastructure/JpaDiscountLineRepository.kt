package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import org.springframework.data.jpa.repository.JpaRepository

interface JpaDiscountLineRepository: JpaRepository<DiscountLine, Long> {
}