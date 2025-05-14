package kr.hhplus.be.server.shared.exception

abstract class BusinessException(val errorCode: ErrorCode): RuntimeException(errorCode.message) {
}