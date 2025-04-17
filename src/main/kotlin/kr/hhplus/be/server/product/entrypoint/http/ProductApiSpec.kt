package kr.hhplus.be.server.product.entrypoint.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.common.CommonResponse
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "상품 조회 및 인기 상품")
interface ProductApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이지 단위로 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = ProductResponse.Retrieve.Lists::class))])
        ]
    )
    @GetMapping
    fun retrieveLists(
        pageable: Pageable,
        @RequestParam keyword: String? = null
    ): ResponseEntity<CommonResponse<ProductResponse.Retrieve.Lists>>

    @Operation(
        summary = "인기 상품 조회",
        description = "최근 3일간 상위 5개 인기 상품을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(array = ArraySchema(schema = Schema(implementation = ProductResponse.Retrieve.Popular::class)))])
        ]
    )
    @GetMapping("/popular")
    fun retrievePopular(): ResponseEntity<CommonResponse<List<ProductResponse.Retrieve.Popular>>>
}