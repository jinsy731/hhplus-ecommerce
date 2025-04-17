package kr.hhplus.be.server.common

import java.time.LocalDateTime

interface ClockHolder {
    fun getNowInLocalDateTime(): LocalDateTime
}