package kr.hhplus.be.server.coupon.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class InvalidCouponStatus: BusinessException(ErrorCode.INvVALID_COUPON_STATUS)