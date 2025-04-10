package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import kr.hhplus.be.server.coupon.domain.model.DiscountLine
import kr.hhplus.be.server.coupon.domain.model.DiscountMethod
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CouponService(
    private val userCouponRepository: UserCouponRepository,
    private val discountLineRepository: DiscountLineRepository,
    ) {

    /**
     * 쿠폰 적용 메서드
     * 1. 할인 적용 대상 필터링
     * 2. 적용 대상 전체에 대한 할인 금액 계산
     * 3. 각 대상에 할인 금액 분배 (물품별 할인 금액 계산을 위해)
     */
    fun applyCoupon(cmd: CouponCommand.ApplyToOrder): CouponResult.ApplyToOrder {
        val userCoupons = userCouponRepository.findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)
        val discountLines  = userCoupons.flatMap { userCoupon ->
            val applicableItems = filterApplicableItems(userCoupon, cmd.order, cmd.userId)
            if (applicableItems.isNotEmpty()) userCoupon.use(cmd.now)
            val totalDiscount = userCoupon.coupon.calculateDiscount(cmd.now, applicableItems.sumOf { it.subTotal() })
            distributeDiscount(applicableItems, userCoupon.id!!, cmd.now, totalDiscount)
        }

        discountLineRepository.saveAll(discountLines)

        return CouponResult.ApplyToOrder(discountLines)
    }

        private fun filterApplicableItems(userCoupon: UserCoupon, order: Order, userId: Long): List<OrderItem> {
            return order.orderItems.filter { orderItem ->
                userCoupon.coupon.isApplicableTo(
                    DiscountContext(
                        userId = userId,
                        productId = orderItem.productId,
                        variantId = orderItem.variantId,
                        orderAmount = order.originalTotal
                    )
                )
            }
        }

        fun distributeDiscount(
            items: List<OrderItem>,
            sourceId: Long,
            now: LocalDateTime,
            totalDiscount: BigDecimal
        ): List<DiscountLine> {
            val total = items.sumOf { it.subTotal() }
            var remaining = totalDiscount
            val result = mutableListOf<DiscountLine>()

            for ((index, item) in items.withIndex()) {
                val itemTotal = item.subTotal()
                val proportion = itemTotal.divide(total)
                val itemDiscount = if (index == items.lastIndex)
                    remaining // 마지막은 남은 전부
                else
                    (totalDiscount * proportion).also { remaining -= it }

                result.add(
                    DiscountLine(
                        orderItemId = item.id,
                        type = DiscountMethod.COUPON,
                        sourceId = sourceId,
                        amount = itemDiscount,
                        createdAt = now
                    )
                )
            }

            return result
        }
    }
