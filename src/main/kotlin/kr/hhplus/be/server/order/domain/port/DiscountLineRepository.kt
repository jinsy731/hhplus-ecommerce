package kr.hhplus.be.server.order.domain.port

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscountLineRepository : JpaRepository<DiscountLine, Long> {

    /**
     * 주문 ID로 할인 내역 목록 조회
     */
    fun findByOrderId(orderId: Long): List<DiscountLine>

    /**
     * 주문 ID 목록으로 할인 내역 목록 조회
     */
    fun findByOrderIdIn(orderIds: List<Long>): List<DiscountLine>

    /**
     * 할인 유형과 출처 ID로 할인 내역 목록 조회
     */
    fun findByTypeAndSourceId(type: DiscountMethod, sourceId: Long): List<DiscountLine>
}