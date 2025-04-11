package kr.hhplus.be.server.common

import org.springframework.data.domain.Page

data class PaginationResult(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun of(page: Page<*>): PaginationResult {
            return PaginationResult(
                page = page.pageable.pageNumber,
                size = page.pageable.pageSize,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}