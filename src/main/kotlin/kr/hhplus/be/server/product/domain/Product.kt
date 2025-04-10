package kr.hhplus.be.server.product.domain

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    val id: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var basePrice: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.DRAFT,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val optionSpecs: MutableList<OptionSpec> = mutableListOf(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val variants: MutableList<ProductVariant> = mutableListOf()
) : BaseTimeEntity() {

    fun addOptionSpec(optionSpec: OptionSpec) {
        optionSpecs.add(optionSpec)
        optionSpec.product = this
    }

    fun addVariant(variant: ProductVariant) {
        variants.add(variant)
        variant.product = this
    }
}

enum class ProductStatus {
    DRAFT, ON_SALE, OUT_OF_STOCK, PARTIALLY_OUT_OF_STOCK, HIDDEN, DISCONTINUED
}
