package kr.hhplus.be.server.product.domain

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity
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
    var additionalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VariantStatus = VariantStatus.ACTIVE,

    @Column(nullable = false)
    var stock: Int,

    @ManyToMany
    @JoinTable(
        name = "variant_option_values",
        joinColumns = [JoinColumn(name = "variant_id")],
        inverseJoinColumns = [JoinColumn(name = "option_value_id")]
    )
    val optionValues: MutableSet<OptionValue> = mutableSetOf()
) : BaseTimeEntity() {

    fun addOptionValue(optionValue: OptionValue) {
        optionValues.add(optionValue)
    }
}

enum class VariantStatus {
    ACTIVE, OUT_OF_STOCK, HIDDEN, DISCONTINUED, DELETED
}
