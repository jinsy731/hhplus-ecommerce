package kr.hhplus.be.server.common

import org.springframework.data.domain.Page

data class Pagination(
    val offset: Long,
    val limit: Long,
    val sort: Array<String>
)

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