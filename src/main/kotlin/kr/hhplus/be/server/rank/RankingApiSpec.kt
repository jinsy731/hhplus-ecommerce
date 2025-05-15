package kr.hhplus.be.server.rank

import org.springframework.http.ResponseEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.shared.web.ErrorResponse

@Tag(name = "랭킹", description = "랭킹 API")
interface RankingApiSpec {

    @Operation(
        summary = "상품 랭킹 조회",
        description = "최근 3일 기준 상위 5개 인기 상품을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = RankingResponse.RetrieveTopProducts.Root::class))]),
            ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
        ]
    )
    fun retrieveTopProducts(): ResponseEntity<CommonResponse<RankingResponse.RetrieveTopProducts.Root>>
}