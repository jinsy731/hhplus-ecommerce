package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.DiscountLine

interface DiscountLineRepository {
    fun saveAll(discountLines: List<DiscountLine>)
}