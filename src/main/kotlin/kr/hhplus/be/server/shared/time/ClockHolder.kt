package kr.hhplus.be.server.shared.time

import java.time.LocalDateTime

interface ClockHolder {
    fun getNowInLocalDateTime(): LocalDateTime
}