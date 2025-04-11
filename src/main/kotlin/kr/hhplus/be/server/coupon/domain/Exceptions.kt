package kr.hhplus.be.server.coupon.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class InvalidCouponStatusException: BusinessException(ErrorCode.INVALID_COUPON_STATUS)
class ExpiredCouponException: BusinessException(ErrorCode.EXPIRED_COUPON)
class AlreadyPaidException: BusinessException(ErrorCode.ALREADY_PAID)
class ExceededMaxCouponLimitException: BusinessException(ErrorCode.EXCEEDED_MAX_COUPON_LIMIT)