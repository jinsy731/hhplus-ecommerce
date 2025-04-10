package kr.hhplus.be.server.coupon.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class InvalidCouponStatusException: BusinessException(ErrorCode.INVALID_COUPON_STATUS)
class ExpiredCouponException: BusinessException(ErrorCode.EXPIRED_COUPON)