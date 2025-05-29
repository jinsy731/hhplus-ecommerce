package kr.hhplus.be.server.product.application.port

import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.application.dto.ProductResult
import kr.hhplus.be.server.product.domain.product.model.Product

interface ProductApplicationService {
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList
    fun retrievePopular(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct>
    fun findAllById(productIds: List<Long>): List<Product>
    fun validateAndReduceStock(cmd: ProductCommand.ValidateAndReduceStock.Root): Result<Unit>
    fun restoreStock(cmd: ProductCommand.RestoreStock.Root)
} 