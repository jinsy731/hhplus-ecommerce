package kr.hhplus.be.server.rank.infrastructure.persistence

import java.time.LocalDate

interface ProductRankingRepository {
    fun increaseRanking(date: LocalDate, productId: Long, quantity: Int)
    fun getTopN(from: LocalDate, to: LocalDate, topN: Long): List<Long>
}