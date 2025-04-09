package kr.hhplus.be.server.common

import jdk.internal.joptsimple.internal.Messages.message

class BusinessException(val errorCode: ErrorCode): RuntimeException(errorCode.message) {
}