package kr.hhplus.be.server.coupon.application

import kr.hhplus.be.server.common.ClockHolder
import kr.hhplus.be.server.common.PageResult
import kr.hhplus.be.server.common.exception.DuplicateCouponIssueException
import kr.hhplus.be.server.coupon.domain.port.CouponRepository
import kr.hhplus.be.server.coupon.domain.port.DiscountLineRepository
import kr.hhplus.be.server.coupon.domain.port.UserCouponRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val discountLineRepository: DiscountLineRepository,
    private val clockHolder: ClockHolder
    ) {

    @Transactional
    fun issueCoupon(cmd: CouponCommand.Issue): CouponResult.Issue {
        userCouponRepository.findByUserIdAndCouponId(cmd.userId, cmd.couponId)
            ?.let { throw DuplicateCouponIssueException() }

        val coupon = couponRepository.getByIdForUpdate(cmd.couponId)
        val userCoupon = coupon.issueTo(cmd.userId, clockHolder.getNowInLocalDateTime())

        val savedUserCoupon = userCouponRepository.save(userCoupon)

        return CouponResult.Issue(
            userCouponId = savedUserCoupon.id,
            status = savedUserCoupon.status,
            expiredAt = savedUserCoupon.expiredAt
        )
    }

    /**
     * 쿠폰 적용 메서드
     * 1. 할인 적용 대상 필터링
     * 2. 적용 대상 전체에 대한 할인 금액 계산
     * 3. 각 대상에 할인 금액 분배 (물품별 할인 금액 계산을 위해)
     */
    @Transactional
    fun use(cmd: CouponCommand.Use.Root): CouponResult.Use {
        val userCoupons = userCouponRepository.findAllByUserIdAndIdIsIn(cmd.userId, cmd.userCouponIds)

        val discountLines = userCoupons.flatMap {
            it.calculateDiscountAndUse(cmd.toDiscountContext())
        }

        val savedDiscountLine = discountLineRepository.saveAll(discountLines)

        return CouponResult.Use(savedDiscountLine.toDiscountInfoList())
    }

    @Transactional(readOnly = true)
    fun retrieveLists(userId: Long, pageable: Pageable): CouponResult.RetrieveList {
        val userCouponPage = userCouponRepository.findAllByUserId(userId, pageable)

        return CouponResult.RetrieveList(
            coupons = userCouponPage.content.map { uc -> uc.toUserCouponData() },
            pageResult = PageResult.of(userCouponPage)
        )
    }
}
