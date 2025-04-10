package kr.hhplus.be.server.order.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class EmptyOrderItemException(): BusinessException(ErrorCode.EMPTY_ORDER_ITEM)
class AlreadyPaidOrderException(): BusinessException(ErrorCode.ALREADY_PAID_ORDER)