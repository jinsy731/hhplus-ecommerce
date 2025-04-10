package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.coupon.application.CouponResult.CouponDiscountPerItem
import kr.hhplus.be.server.coupon.domain.model.DiscountContext
import kr.hhplus.be.server.coupon.domain.model.UserCoupon
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.collections.lastIndex
import kotlin.collections.withIndex

@Service
class CouponService(private val userCouponRepository: UserCouponRepository) {

    fun applyCoupon(cmd: CouponCommand.ApplyToOrder): CouponResult.ApplyToOrder {
        val userCoupons = userCouponRepository.findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)
        val totalItemDiscounts = mutableListOf<CouponDiscountPerItem>()
        userCoupons.forEach { userCoupon ->
            val applicableItems = filterApplicableItems(userCoupon, cmd.order, cmd.userId)
            if(applicableItems.isNotEmpty()) userCoupon.use(cmd.now)
            val totalDiscount = userCoupon.coupon.calculateDiscount(cmd.now, applicableItems.sumOf { it.subTotal() })
            totalItemDiscounts.addAll(distributeDiscount(applicableItems, totalDiscount))
        }

        return CouponResult.ApplyToOrder(
            totalDiscount = totalItemDiscounts.sumOf { it.discountAmount },
            discountPerItem = totalItemDiscounts
        )
    }

    private fun filterApplicableItems(userCoupon: UserCoupon, order: Order, userId: Long): List<OrderItem> {
        return order.orderItems.filter { orderItem ->
            userCoupon.coupon.isApplicableTo(DiscountContext(
                userId = userId,
                productId = orderItem.productId,
                variantId = orderItem.variantId,
                orderAmount = order.originalTotal))
        }
    }

    fun distributeDiscount(
        items: List<OrderItem>,
        totalDiscount: BigDecimal
    ): List<CouponDiscountPerItem> {
        val total = items.sumOf { it.subTotal() }
        var remaining = totalDiscount
        val result = mutableListOf<CouponDiscountPerItem>()

        for ((index, item) in items.withIndex()) {
            val itemTotal = item.subTotal()
            val proportion = itemTotal.divide(total)
            val itemDiscount = if (index == items.lastIndex)
                remaining // 마지막은 남은 전부
            else
                (totalDiscount * proportion).also { remaining -= it }

            result.add(CouponDiscountPerItem(item.id, itemDiscount))
        }

        return result
    }
}