package kr.hhplus.be.server.common

data class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null
)

data class PageInfo(
    val page: Int,
    val size: Int,
    val totalElement: Int,
    val totalPages: Int
)
