package kr.hhplus.be.server.product.domain.product.model

import jakarta.persistence.*
import kr.hhplus.be.server.shared.domain.BaseTimeEntity
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.VariantOutOfStockException
import kr.hhplus.be.server.shared.exception.VariantUnavailableException
import kr.hhplus.be.server.shared.persistence.LongSetConverter
import org.slf4j.LoggerFactory

@Entity
@Table(name = "product_variants")
class ProductVariant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var product: Product? = null,

    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "additional_price"))
    var additionalPrice: Money = Money.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VariantStatus = VariantStatus.ACTIVE,

    @Column(nullable = false)
    var stock: Int,

    @Column(nullable = false) @Convert(converter = LongSetConverter::class)
    val optionValues: MutableSet<Long> = mutableSetOf()
) : BaseTimeEntity() {
    @Transient
    private val logger = LoggerFactory.getLogger(ProductVariant::class.java)

    fun checkAvailableToOrder(quantity: Int) {
        check(this.status == VariantStatus.ACTIVE) { throw VariantUnavailableException() }
        check(this.stock >= quantity) { throw VariantOutOfStockException() }
    }

    fun reduceStock(quantity: Int) {
        check(this.stock >= quantity) { throw VariantOutOfStockException()  }
        this.stock -= quantity
        logger.info("stock after reduce(ID: $id): $stock")
    }

    fun restoreStock(quantity: Int) {
        this.stock += quantity
        logger.info("stock after restore(ID: $id): $stock")
    }
}

enum class VariantStatus {
    ACTIVE, OUT_OF_STOCK, HIDDEN, DISCONTINUED, DELETED
}
