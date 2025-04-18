package kr.hhplus.be.server.product.domain.stats

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class ProductSalesAggregationDailyId(
    val productId: Long,
    val salesDay: LocalDate
): Serializable

@Entity
@Table(name = "P_SALES_AGG_DAY")
class ProductSalesAggregationDaily(
    @EmbeddedId
    val id: ProductSalesAggregationDailyId,
    @Column(nullable = false)
    var salesCount: Long
) {
    fun accumulate(increment: Long): ProductSalesAggregationDaily {
        this.salesCount += increment
        return this
    }
}