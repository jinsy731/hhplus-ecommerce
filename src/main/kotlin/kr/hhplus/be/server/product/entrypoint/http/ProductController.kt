package kr.hhplus.be.server.product.entrypoint.http

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.common.PageInfo
import kr.hhplus.be.server.product.domain.product.ProductStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/products")
class ProductController: ProductApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): CommonResponse<ProductResponse.Retrieve.Lists> {
        // 새로운 형식에 맞게 샘플 데이터 구성
        val result = ProductResponse.Retrieve.Lists(
            products = listOf(
                ProductResponse.ProductDetail(
                    productId = 1,
                    name = "티셔츠",
                    basePrice = BigDecimal(29000),
                    status = ProductStatus.ON_SALE,
                    optionSpecs = listOf(
                        ProductResponse.OptionSpecDetail(
                            id = 1,
                            name = "색상",
                            displayOrder = 1,
                            values = listOf(
                                ProductResponse.OptionValueDetail(id = 11, value = "검정"),
                                ProductResponse.OptionValueDetail(id = 12, value = "회색")
                            )
                        ),
                        ProductResponse.OptionSpecDetail(
                            id = 2,
                            name = "사이즈",
                            displayOrder = 2,
                            values = listOf(
                                ProductResponse.OptionValueDetail(id = 13, value = "S"),
                                ProductResponse.OptionValueDetail(id = 14, value = "M")
                            )
                        )
                    ),
                    variants = listOf(
                        ProductResponse.ProductVariantDetail(
                            variantId = 101,
                            optionValueIds = listOf(11, 13),
                            additionalPrice = 1000,
                            status = "ACTIVE",
                            stock = 10
                        )
                    )
                )
            ),
            pageInfo = PageInfo(page, size, 31, 4)
        )
        return CommonResponse(result)
    }

    @GetMapping("/popular")
    override fun getPopularProducts(): CommonResponse<List<ProductResponse.Retrieve.Popular>> {
        val result = listOf(ProductResponse.Retrieve.Popular(1, "반팔티", 37))
        return CommonResponse(result)
    }
}
