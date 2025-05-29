package kr.hhplus.be.server.product.domain.stats

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class ProductSalesAggregationDailyCheckpoint(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
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