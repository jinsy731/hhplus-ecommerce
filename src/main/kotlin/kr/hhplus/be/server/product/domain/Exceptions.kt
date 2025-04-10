package kr.hhplus.be.server.product.domain

import kr.hhplus.be.server.common.BusinessException
import kr.hhplus.be.server.common.ErrorCode

class VariantUnavailableException(): BusinessException(ErrorCode.VARIANT_UNAVAILABLE)
class VariantOutOfStockException(): BusinessException(ErrorCode.VARIANT_OUT_OF_STOCK)
class ProductUnavailableException(): BusinessException(ErrorCode.PRODUCT_UNAVAILABLE)