package kr.hhplus.be.server.common.exception

abstract class BusinessException(val errorCode: ErrorCode): RuntimeException(errorCode.message) {
}