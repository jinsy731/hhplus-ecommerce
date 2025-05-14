package kr.hhplus.be.server.shared.time

import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DefaultClockHolder: ClockHolder {
    override fun getNowInLocalDateTime(): LocalDateTime {
        return LocalDateTime.now()
    }
}