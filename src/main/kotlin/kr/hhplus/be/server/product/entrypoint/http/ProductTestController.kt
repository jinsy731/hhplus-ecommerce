package kr.hhplus.be.server.product.entrypoint.http

import kr.hhplus.be.server.product.domain.product.model.*
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.web.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/test/products")
class ProductTestController(
    private val productRepository: ProductRepository
) {

    @PostMapping("/simple")
    @Transactional
    fun createSimpleProduct(
        @RequestBody request: CreateSimpleProductRequest
    ): ResponseEntity<CommonResponse<CreateProductResponse>> {
        val product = Product(
            name = request.name,
            basePrice = Money.of(request.basePrice),
            status = ProductStatus.ON_SALE
        )

        // 기본 variant 추가 (옵션 없는 기본 상품)
        val variant = ProductVariant(

            additionalPrice = Money.ZERO,
            status = VariantStatus.ACTIVE,
            stock = request.stock
        )
        product.addVariant(variant)

        val savedProduct = productRepository.save(product)
        
        return ResponseEntity.ok(
            CommonResponse(
                CreateProductResponse(
                    productId = savedProduct.id!!,
                    variantId = variant.id!!
                )
            )
        )
    }
}

data class CreateSimpleProductRequest(
    val name: String,
    val basePrice: BigDecimal,
    val stock: Int
)

data class CreateProductResponse(
    val productId: Long,
    val variantId: Long,
)
