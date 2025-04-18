package kr.hhplus.be.server.product.domain.stats

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class PopularProductDailyId(
    @Column(name = "`sales_day`")
    val salesDay: LocalDate,
    @Column(name = "`rank`")
    val rank: Int
): Serializable

@Entity
@Table(name = "popular_products_daily")
class PopularProductsDaily(
    @EmbeddedId
    val id: PopularProductDailyId,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val totalSales: Long
)
