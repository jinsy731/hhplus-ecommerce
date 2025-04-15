package kr.hhplus.be.server.order.application.dto

import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.product.domain.ProductVariant
import java.math.BigDecimal

/**
 * Order <-> Product 사이의 도메인 간 결합도를 낮추기 위한 ACL의 역할을 하는 DTO
 * 주문 생성에 필요한 데이터만을 가짐
 * 기능별로 별도의 DTO를 구성 -> 기능 간 DTO를 공유했을 때의 변경의 영향 전파 감소 / Testability 증가
 */
class ProductInfo {
    class CreateOrder {
        data class Root(
            val productId: Long,
            val variants: List<Variant>
        )
        data class Variant(
            val variantId: Long,
            val unitPrice: BigDecimal
        )
    }
}

fun List<Product>.toCreateOrderProductInfo(): List<ProductInfo.CreateOrder.Root> {
    return this.map { product -> ProductInfo.CreateOrder.Root(
        productId = product.id!!,
        variants = product.variants.toCreateOrderProductInfo(product)
    ) }
}

fun List<ProductVariant>.toCreateOrderProductInfo(product: Product): List<ProductInfo.CreateOrder.Variant> {
    return this.map { variant -> ProductInfo.CreateOrder.Variant(
        variantId = variant.id,
        unitPrice = product.getVariantPrice(variant.id)
    ) }
}