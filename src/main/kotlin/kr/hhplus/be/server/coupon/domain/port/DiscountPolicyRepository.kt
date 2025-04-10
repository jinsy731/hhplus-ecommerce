package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.DiscountPolicy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscountPolicyRepository : JpaRepository<DiscountPolicy, Long> {
    fun findByName(name: String): DiscountPolicy?
}
