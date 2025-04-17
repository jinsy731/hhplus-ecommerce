package kr.hhplus.be.server.common

import org.springframework.data.domain.Page

data class PageResult(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun of(page: Page<*>): PageResult {
            return PageResult(
                page = page.pageable.pageNumber,
                size = page.pageable.pageSize,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}

fun PageResult.toResponse() = PageInfo(
    page = this.page,
    size = this.size,
    totalElement = this.totalElements,
    totalPages = this.totalPages
)