package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.common.PageResult
import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalStateException
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository
) {


    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByNameContaining(cmd.keyword, cmd.lastId, cmd.pageable) // TODO: pageable 말고 Spring 의존적이지 않은 파라미터를 써야하나 ?
        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() },
        )
    }

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)

    fun validatePurchasability(cmd: ProductCommand.ValidatePurchasability.Root) {
        val products = productRepository.findAll(cmd.items.map { it.productId })

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            product.validatePurchasability(item.variantId, item.quantity)
        }
    }

    @Transactional
    fun reduceStockByPurchase(cmd: ProductCommand.ReduceStockByPurchase.Root) {
        val products = productRepository.findAll(cmd.items.map { it.productId })

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            product.reduceStockByPurchase(item.variantId, item.quantity)
        }
    }
    

    fun retrievePopular(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct> {
        val today = LocalDate.now()
        val salesAggregates = popularProductsDailyRepository.findAllById(
            (1..5).map { PopularProductDailyId(today, it) }
        )
        val products = productRepository.findAll(salesAggregates.map { it.productId })
        
        return salesAggregates.map { aggregate ->
            ProductResult.PopularProduct(
                productId = aggregate.productId,
                name = products.find { it.id == aggregate.productId }?.name ?: throw IllegalStateException(),
                totalSales = aggregate.totalSales.toInt()
            )
        }
    }
}