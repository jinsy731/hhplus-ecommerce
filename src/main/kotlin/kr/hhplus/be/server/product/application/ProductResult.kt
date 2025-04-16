package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.common.PaginationResult
import kr.hhplus.be.server.product.domain.product.Product

class ProductResult {
    data class RetrieveList(
        val products: List<Product>,
        val paginationResult: PaginationResult
    )
}