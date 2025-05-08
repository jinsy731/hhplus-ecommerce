package kr.hhplus.be.server.shared.exception

// Coupon
class InvalidCouponStatusException: BusinessException(ErrorCode.INVALID_COUPON_STATUS)
class ExpiredCouponException: BusinessException(ErrorCode.EXPIRED_COUPON)
class ExceededMaxCouponLimitException: BusinessException(ErrorCode.EXCEEDED_MAX_COUPON_LIMIT)
class CouponTargetNotFoundException: BusinessException(ErrorCode.COUPON_TARGET_NOT_FOUND)
class DuplicateCouponIssueException: BusinessException(ErrorCode.DUPLICATE_COUPON_ISSUE)


// Payment
class AlreadyPaidException: BusinessException(ErrorCode.ALREADY_PAID)

// Order
class EmptyOrderItemException(): BusinessException(ErrorCode.EMPTY_ORDER_ITEM)
class AlreadyPaidOrderException(): BusinessException(ErrorCode.ALREADY_PAID_ORDER)

// Product
class VariantUnavailableException(): BusinessException(ErrorCode.VARIANT_UNAVAILABLE)
class VariantOutOfStockException(): BusinessException(ErrorCode.VARIANT_OUT_OF_STOCK)
class ProductUnavailableException(): BusinessException(ErrorCode.PRODUCT_UNAVAILABLE)

// Point
class InvalidChargeAmountException(): BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT)
class InsufficientPointException(): BusinessException(ErrorCode.INSUFFICIENT_POINT)

// Common
class ResourceNotFoundException(): BusinessException(ErrorCode.NOT_FOUND)