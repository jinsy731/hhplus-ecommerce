package kr.hhplus.be.server.rank.application

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingKeyGenerator {
    fun generateDailyKey(date: LocalDate): String {
        val formattedDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "ranking:product:daily:${formattedDate}"
    }
    fun generateUnionKey(from: LocalDate, to: LocalDate): String {
        val formattedFrom = from.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val formattedTo = to.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "ranking:product:union:${formattedFrom}:${formattedTo}"
    }
}