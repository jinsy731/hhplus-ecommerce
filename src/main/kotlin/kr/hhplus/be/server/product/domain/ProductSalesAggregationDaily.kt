package kr.hhplus.be.server.product.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class ProductSalesAggregationDailyId(
    val productId: Long,
    val salesDay: LocalDate
): Serializable

@Entity
class ProductSalesAggregationDaily(
    @EmbeddedId
    val id: ProductSalesAggregationDailyId,
    @Column(nullable = false)
    val salesCount: Long
)