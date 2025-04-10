package kr.hhplus.be.server.user.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class InvalidChargeAmountException(): BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT)
class InsufficientPointException(): BusinessException(ErrorCode.INSUFFICIENT_POINT)