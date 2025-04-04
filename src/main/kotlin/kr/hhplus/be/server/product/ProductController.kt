package kr.hhplus.be.server.product

import kr.hhplus.be.server.common.ApiResponse
import kr.hhplus.be.server.common.PageInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<ProductListResponse> {
        val result = ProductListResponse(
            listOf(
                ProductResponse(
                    1, "티셔츠", listOf(ProductVariantResponse(101, "검정 / L", 20000, 10))
                )
            ),
            PageInfo(page, size, 31, 4)
        )
        return ApiResponse("SUCCESS", "상품 목록이 조회되었습니다.", result)
    }

    @GetMapping("/popular")
    fun getPopularProducts(): ApiResponse<List<PopularProductResponse>> {
        val result = listOf(PopularProductResponse(1, "반팔티", 37))
        return ApiResponse("SUCCESS", "인기 상품 조회 성공", result)
    }
}
