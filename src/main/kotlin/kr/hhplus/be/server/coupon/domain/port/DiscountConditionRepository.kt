package kr.hhplus.be.server.coupon.domain.port

import kr.hhplus.be.server.coupon.domain.model.DiscountCondition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscountConditionRepository : JpaRepository<DiscountCondition, Long>
