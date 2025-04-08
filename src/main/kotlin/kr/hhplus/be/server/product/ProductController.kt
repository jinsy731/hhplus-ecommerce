package kr.hhplus.be.server.product

import kr.hhplus.be.server.common.CommonResponse
import kr.hhplus.be.server.common.PageInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductController: ProductApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): CommonResponse<ProductListResponse> {
        // 새로운 형식에 맞게 샘플 데이터 구성
        val result = ProductListResponse(
            products = listOf(
                ProductResponse(
                    productId = 1,
                    name = "티셔츠",
                    basePrice = 29000,
                    status = "ON_SALE",
                    optionSpecs = listOf(
                        OptionSpecResponse(
                            id = 1,
                            name = "색상",
                            displayOrder = 1,
                            values = listOf(
                                OptionValueResponse(id = 11, value = "검정"),
                                OptionValueResponse(id = 12, value = "회색")
                            )
                        ),
                        OptionSpecResponse(
                            id = 2,
                            name = "사이즈",
                            displayOrder = 2,
                            values = listOf(
                                OptionValueResponse(id = 13, value = "S"),
                                OptionValueResponse(id = 14, value = "M")
                            )
                        )
                    ),
                    variants = listOf(
                        ProductVariantResponse(
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
        return CommonResponse("SUCCESS", "상품 목록이 조회되었습니다.", result)
    }

    @GetMapping("/popular")
    override fun getPopularProducts(): CommonResponse<List<PopularProductResponse>> {
        val result = listOf(PopularProductResponse(1, "반팔티", 37))
        return CommonResponse("SUCCESS", "인기 상품 조회 성공", result)
    }
}
