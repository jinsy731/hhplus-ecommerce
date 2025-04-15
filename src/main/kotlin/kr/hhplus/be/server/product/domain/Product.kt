package kr.hhplus.be.server.product.domain

import jakarta.persistence.*
import kr.hhplus.be.server.common.exception.ProductUnavailableException
import kr.hhplus.be.server.common.entity.BaseTimeEntity
import java.math.BigDecimal

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
    var basePrice: BigDecimal,

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

    /**
     * 단순 수량에 대한 검증만 하는 것이 아니므로 네이밍을 purchase로 가져감.
     * (상품이 구매 가능한 상태인지도 검증하기 때문에..)
     */
    fun reduceStockByPurchase(variantId: Long, quantity: Int) {
        validatePurchasability(variantId, quantity)
        findVariant(variantId).reduceStock(quantity)
    }

    fun validatePurchasability(variantId: Long, quantity: Int) {
        check(this.status in setOf(ProductStatus.ON_SALE, ProductStatus.PARTIALLY_OUT_OF_STOCK)) { throw ProductUnavailableException() }
        findVariant(variantId)?.checkAvailableToOrder(quantity)
    }

    fun getVariantPrice(variantId: Long): BigDecimal {
        return (this.basePrice + findVariant(variantId).additionalPrice)
    }

    private fun findVariant(variantId: Long): ProductVariant = this.variants.find { it.id == variantId } ?: throw IllegalArgumentException("존재하지 않는 옵션입니다.")
}
