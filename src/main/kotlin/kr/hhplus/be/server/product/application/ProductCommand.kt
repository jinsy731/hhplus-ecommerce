package kr.hhplus.be.server.product.application

import org.springframework.data.domain.Pageable
import java.time.LocalDate

class ProductCommand {
    data class RetrieveList(
        val pageable: Pageable,
        val lastId: Long?,
        val keyword: String?
    )
    
    data class RetrievePopularProducts(
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val limit: Int
    )
    
    class ValidatePurchasability {
        data class Root(
            val items: List<Item>
        )
        data class Item(
            val productId: Long,
            val variantId: Long,
            val quantity: Int
        )
    }

    class ReduceStockByPurchase {
        data class Root(
            val items: List<Item>
        )
        data class Item(
            val productId: Long,
            val variantId: Long,
            val quantity: Int
        )
    }
}