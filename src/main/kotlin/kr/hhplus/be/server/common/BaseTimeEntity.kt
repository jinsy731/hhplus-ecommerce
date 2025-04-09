package kr.hhplus.be.server.common

import java.time.LocalDateTime

abstract class BaseTimeEntity {
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
}