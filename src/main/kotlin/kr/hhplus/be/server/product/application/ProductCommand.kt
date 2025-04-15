package kr.hhplus.be.server.product.application

import org.springframework.data.domain.Pageable

class ProductCommand {
    data class RetrieveList(
        val pageable: Pageable,
        val keyword: String
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