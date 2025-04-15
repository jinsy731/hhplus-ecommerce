package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import java.math.BigDecimal

/**
 * 할인 정보를 나타내는 인터페이스
 * Coupon 도메인과의 의존성을 줄이기 위한 추상화
 */
data class DiscountInfo (
    val orderItemId: Long,
    val amount: BigDecimal,
    val sourceId: Long,
    val sourceType: String
)

fun List<DiscountLine>.toDiscountInfoList(): List<DiscountInfo> {
    return this.mapNotNull { DiscountInfo(
        orderItemId = it.id,
        amount = it.amount,
        sourceId = it.sourceId,
        sourceType = it.type.toString()
    ) }
}
