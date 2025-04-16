package kr.hhplus.be.server.product.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class ProductSalesAggregationCheckpoint(
    @Id
    val lastAggregatedLogId: Long,
    @Column(nullable = false)
    val lastAggregatedAt: LocalDateTime
)