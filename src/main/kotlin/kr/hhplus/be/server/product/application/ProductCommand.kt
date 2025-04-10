package kr.hhplus.be.server.product.application

import org.springframework.data.domain.Pageable

class ProductCommand {
    data class RetrieveList(
        val pageable: Pageable,
        val keyword: String
    )
}