package kr.hhplus.be.server.product.domain

import jakarta.persistence.*
import kr.hhplus.be.server.common.exception.VariantOutOfStockException
import kr.hhplus.be.server.common.exception.VariantUnavailableException
import kr.hhplus.be.server.common.entity.BaseTimeEntity
import kr.hhplus.be.server.coupon.LongSetConverter
import java.math.BigDecimal

@Entity
@Table(name = "product_variants")
class ProductVariant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(nullable = false)
    var additionalPrice: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VariantStatus = VariantStatus.ACTIVE,

    @Column(nullable = false)
    var stock: Int,

    @Column(nullable = false) @Convert(converter = LongSetConverter::class)
    val optionValues: MutableSet<Long> = mutableSetOf()
) : BaseTimeEntity() {

    fun checkAvailableToOrder(quantity: Int) {
        check(this.status == VariantStatus.ACTIVE) { throw VariantUnavailableException() }
        check(this.stock >= quantity) { throw VariantOutOfStockException() }
    }

    fun reduceStock(quantity: Int) {
        check(this.stock > quantity) { throw VariantOutOfStockException()  }
        this.stock -= quantity
    }
}

enum class VariantStatus {
    ACTIVE, OUT_OF_STOCK, HIDDEN, DISCONTINUED, DELETED
}
