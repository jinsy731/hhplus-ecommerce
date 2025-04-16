package kr.hhplus.be.server.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 응답 포맷")
data class CommonResponse<T>(
    @Schema(description = "응답 데이터")
    val data: T? = null
)

@Schema(description = "페이지 정보")
data class PageInfo(

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,

    @Schema(description = "페이지당 항목 수", example = "20")
    val size: Int,

    @Schema(description = "전체 항목 수", example = "152")
    val totalElement: Int,

    @Schema(description = "전체 페이지 수", example = "8")
    val totalPages: Int
)


@Schema(description = "에러 응답 포맷")
data class ErrorResponse(
    @Schema(description = "에러 코드", example = "INVALID_AMOUNT")
    val code: String,

    @Schema(description = "에러 메시지", example = "충전 금액은 0보다 커야 합니다.")
    val message: String,

    @Schema(description = "필드별 에러 목록")
    val fieldErrors: List<FieldError>? = null
)

@Schema(description = "필드 에러 상세 정보")
data class FieldError(
    @Schema(description = "에러 발생 필드", example = "amount")
    val field: String,
    @Schema(description = "입력값", example = "-1000")
    val value: String,
    @Schema(description = "에러 메시지", example = "0보다 커야 합니다.")
    val errorMessage: String
)
