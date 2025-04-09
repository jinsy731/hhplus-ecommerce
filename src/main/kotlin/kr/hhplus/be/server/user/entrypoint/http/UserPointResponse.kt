package kr.hhplus.be.server.user.entrypoint.http

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class UserPointResponse {

    @Schema(description = "포인트 충전 응답")
    data class Charge(
        @Schema(description = "유저 ID", example = "1")
        val userId: Long,
        @Schema(description = "잔액", example = "12000")
        val point: Long,
        @Schema(description = "최중 수정 시각", example = "2025-05-10'T'10:00:00")
        val updatedAt: LocalDateTime?
    )

    @Schema(description = "포인트 조회 응답")
    data class Retrieve(
        @Schema(description = "유저 ID", example = "1")
        val userId: Long,
        @Schema(description = "잔액", example = "12000")
        val point: Long,
        @Schema(description = "최중 수정 시각", example = "2025-05-10'T'10:00:00")
        val updatedAt: LocalDateTime?
    )
}
