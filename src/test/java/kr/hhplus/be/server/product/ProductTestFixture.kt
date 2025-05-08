package kr.hhplus.be.server.product

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant

/**
 * 상품 관련 테스트 픽스처
 * 테스트 의도에 맞게 상품과 상품 변형을 생성합니다.
 */
object ProductTestFixture {
    
    // 기본 상품 생성 빌더
    fun product(
        id: Long? = null,
        name: String = "테스트 상품",
        basePrice: Money = Money.of(1000),
        status: ProductStatus = ProductStatus.ON_SALE
    ): ProductBuilder {
        return ProductBuilder(
            id = id,
            name = name,
            basePrice = basePrice,
            status = status
        )
    }
    
    // 시나리오별 상품 생성 메서드
    
    /**
     * 판매 중인 상품 생성 (재고 있음)
     */
    fun onSaleProduct(id: Long? = null): Product {
        return product(id = id)
            .withStatus(ProductStatus.ON_SALE)
            .withVariants(
                variant(additionalPrice = Money.of(500), stock = 10),
                variant(additionalPrice = Money.of(100), stock = 10)
            )
            .build()
    }
    
    /**
     * 품절된 상품 생성 (재고 없음)
     */
    fun outOfStockProduct(id: Long? = null): Product {
        return product(id = id)
            .withStatus(ProductStatus.OUT_OF_STOCK)
            .withVariants(
                variant(additionalPrice = Money.of(500), stock = 0),
                variant(additionalPrice = Money.of(100), stock = 0)
            )
            .build()
    }
    
    /**
     * 판매 중지된 상품 생성
     */
    fun discontinuedProduct(id: Long? = null): Product {
        return product(id = id)
            .withStatus(ProductStatus.DISCONTINUED)
            .withVariants(
                variant(additionalPrice = Money.of(500), stock = 10),
                variant(additionalPrice = Money.of(100), stock = 10)
            )
            .build()
    }
    
    /**
     * 프리미엄 상품 생성 (높은 가격)
     */
    fun premiumProduct(id: Long? = null): Product {
        return product(id = id)
            .withName("프리미엄 상품")
            .withBasePrice(Money.of(50000))
            .withVariants(
                variant(additionalPrice = Money.of(10000), stock = 5),
                variant(additionalPrice = Money.of(20000), stock = 3)
            )
            .build()
    }
    
    /**
     * 상품 변형 생성
     */
    fun variant(
        id: Long? = null, 
        additionalPrice: Money = Money.of(500), 
        stock: Int = 10
    ): ProductVariant {
        return ProductVariant(
            id = id,
            additionalPrice = additionalPrice,
            stock = stock
        )
    }
    
    // 빌더 클래스
    class ProductBuilder(
        private val id: Long?,
        private var name: String,
        private var basePrice: Money,
        private var status: ProductStatus
    ) {
        private val variants: MutableList<ProductVariant> = mutableListOf()
        
        fun build(): Product {
            val product = Product(
                id = id,
                name = name,
                basePrice = basePrice,
                status = status
            )
            
            variants.forEach {
                product.addVariant(it)
                it.product = product
            }
            return product
        }
        
        fun withName(name: String): ProductBuilder {
            this.name = name
            return this
        }
        
        fun withBasePrice(basePrice: Money): ProductBuilder {
            this.basePrice = basePrice
            return this
        }
        
        fun withStatus(status: ProductStatus): ProductBuilder {
            this.status = status
            return this
        }
        
        fun withVariant(variant: ProductVariant): ProductBuilder {
            this.variants.add(variant)
            return this
        }
        
        fun withVariants(vararg variants: ProductVariant): ProductBuilder {
            this.variants.addAll(variants)
            return this
        }
    }
    
    // 기존 호환성 유지 메서드들 (리팩토링 기간 동안만 유지)
    @Deprecated("새로운 onSaleProduct() 메서드를 사용하세요", ReplaceWith("onSaleProduct(id)"))
    fun createValidProduct(id: Long? = null, variantIds: List<Long?> = listOf(null, null)): Product {
        val product = Product(
            id = id,
            name = "테스트 상품",
            basePrice = Money.of(1000),
            status = ProductStatus.ON_SALE
        )
        
        variantIds.forEach {
            product.addVariant(createValidVariant(id = it, additionalPrice = Money.of(500)))
            product.addVariant(createValidVariant(id = it, additionalPrice = Money.of(100)))
        }
        
        return product
    }

    @Deprecated("새로운 outOfStockProduct() 메서드를 사용하세요", ReplaceWith("outOfStockProduct(id)"))
    fun createInvalidProduct(id: Long? = null, variantIds: List<Long?> = listOf(null, null), status: ProductStatus = ProductStatus.OUT_OF_STOCK): Product {
        val product = Product(
            id = id,
            name = "구매 불가 상품",
            basePrice = Money.of(1000),
            status = status
        )
        
        variantIds.forEach {
            product.addVariant(createInvalidVariant(id = it, additionalPrice = Money.of(500)))
            product.addVariant(createInvalidVariant(id = it, additionalPrice = Money.of(100)))
        }
        
        return product
    }

    @Deprecated("variant() 메서드를 사용하세요", ReplaceWith("variant(id, additionalPrice, 10)"))
    private fun createValidVariant(id: Long? = null, additionalPrice: Money = Money.of(500)): ProductVariant {
        return ProductVariant(
            id = id,
            additionalPrice = additionalPrice,
            stock = 10
        )
    }

    @Deprecated("variant() 메서드를 사용하세요", ReplaceWith("variant(id, additionalPrice, 0)"))
    private fun createInvalidVariant(id: Long? = null, additionalPrice: Money = Money.of(500)): ProductVariant {
        return ProductVariant(
            id = id,
            additionalPrice = additionalPrice,
            stock = 0
        )
    }
}