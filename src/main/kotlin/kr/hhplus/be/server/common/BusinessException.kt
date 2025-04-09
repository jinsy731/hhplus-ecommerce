package kr.hhplus.be.server.common

abstract class BusinessException(val errorCode: ErrorCode): RuntimeException(errorCode.message) {
}