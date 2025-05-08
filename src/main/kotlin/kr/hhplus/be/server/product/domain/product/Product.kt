package kr.hhplus.be.server.product.domain.product

import jakarta.persistence.*
import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.ProductUnavailableException
import kr.hhplus.be.server.common.entity.BaseTimeEntity

enum class ProductStatus {
    DRAFT,
    ON_SALE,
    OUT_OF_STOCK,
    PARTIALLY_OUT_OF_STOCK,
    HIDDEN,
    DISCONTINUED
}

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "base_price"))
    var basePrice: Money,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.DRAFT,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    val optionSpecs: MutableList<OptionSpec> = mutableListOf(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.PERSIST], orphanRemoval = true, fetch = FetchType.LAZY)
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

    fun reduceStockByPurchase(variantId: Long, quantity: Int) {
        validatePurchasability(variantId, quantity)
        findVariant(variantId).reduceStock(quantity)
    }

    fun validatePurchasability(variantId: Long, quantity: Int) {
        check(this.status in setOf(ProductStatus.ON_SALE, ProductStatus.PARTIALLY_OUT_OF_STOCK)) { throw ProductUnavailableException() }
        findVariant(variantId)?.checkAvailableToOrder(quantity)
    }

    fun getFinalPriceWithVariant(variantId: Long): Money {
        return (this.basePrice + findVariant(variantId).additionalPrice)
    }

    private fun findVariant(variantId: Long): ProductVariant = this.variants.find { it.id == variantId } ?: throw IllegalArgumentException("존재하지 않는 옵션입니다.")
}
