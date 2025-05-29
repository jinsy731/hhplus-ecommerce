package kr.hhplus.be.server.product.domain.stats

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class ProductSalesLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "product_sales_log_id")
    val id: Long? = null,
    @Column(nullable = false)
    val orderId: Long,
    @Column(nullable = false)
    val productId: Long,
    @Column(nullable = false)
    val variantId: Long,
    @Column(nullable = false)
    val quantity: Long,
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    val type: TransactionType,
    @Column(nullable = false)
    val timestamp: LocalDateTime
)

enum class TransactionType {
    SOLD, RETURN
}