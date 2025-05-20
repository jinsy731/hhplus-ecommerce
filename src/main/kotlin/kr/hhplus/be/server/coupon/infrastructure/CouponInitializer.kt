package kr.hhplus.be.server.coupon.infrastructure

import jakarta.annotation.PostConstruct
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponKVStore
import kr.hhplus.be.server.coupon.infrastructure.kvstore.CouponStock
import kr.hhplus.be.server.coupon.infrastructure.persistence.JpaCouponRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CouponInitializer(
    private val couponKVStore: CouponKVStore,
    private val couponRepository: JpaCouponRepository
) {

    @PostConstruct
    fun init() {
        var page = 0
        val size = 1000
        while (true) {
            val page = couponRepository.findAll(PageRequest.of(page++, size))
            val content = page.content
            if (content.isEmpty()) {
                break
            }
            content.forEach { coupon ->
                couponKVStore.setStock(CouponStock(coupon.id!!, coupon.maxIssueLimit.toLong()))
            }
        }
    }
}