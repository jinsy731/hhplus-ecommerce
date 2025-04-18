package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultDiscountLineRepository(private val jpaRepository: JpaDiscountLineRepository): DiscountLineRepository {
    override fun saveAll(discountLines: List<DiscountLine>): List<DiscountLine> {
        return jpaRepository.saveAll(discountLines)
    }
}