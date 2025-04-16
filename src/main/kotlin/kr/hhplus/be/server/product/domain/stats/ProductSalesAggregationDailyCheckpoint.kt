package kr.hhplus.be.server.product.domain.stats

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class ProductSalesAggregationDailyCheckpoint(
    @Id
    val lastAggregatedLogId: Long,
    @Column(nullable = false)
    val lastAggregatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun toString(): String {
        return """
            lastAggregatedLodId = ${this.lastAggregatedLogId}
        """.trimIndent()
    }
}