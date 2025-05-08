package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository,
) {
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable)

        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
        )
    }

    @Cacheable(
        cacheNames = ["productSearch"],
        key = "'cache:product:search:' + (#cmd.keyword != null ? #cmd.keyword : 'all') + ':' + (#cmd.lastId != null ? #cmd.lastId : ${Int.MAX_VALUE}) + ':' + #cmd.pageable.pageSize"
    )
    fun retrieveListWithPageCache(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList =
        retrieveList(cmd)

    @WithMultiDistributedLock(
        keys = [
            "#cmd.items.![ 'product:' + #this.productId ]",
            "#cmd.items.![ 'variant:' + #this.variantId ]"
       ],
        type = LockType.PUBSUB,
        waitTimeMillis = 4000
    )
    @Transactional
    fun validateAndReduceStock(cmd: ProductCommand.ValidateAndReduceStock.Root) {
        val products = productRepository.findAllByIdForUpdate(cmd.items.map { it.productId }.sorted())

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            product.validatePurchasability(item.variantId, item.quantity)
            product.reduceStockByPurchase(item.variantId, item.quantity)
        }
    }

    fun retrievePopular(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct> {
        val salesAggregates = popularProductsDailyRepository.findAllById(
            (1..cmd.limit).map { PopularProductDailyId(cmd.toDate, it) }
        ).toList()
        val products = productRepository.findAll(salesAggregates.map { it.productId })
        
        return salesAggregates.map { aggregate ->
            ProductResult.PopularProduct(
                productId = aggregate.productId,
                name = products.find { it.id == aggregate.productId }?.name ?: throw IllegalStateException(),
                totalSales = aggregate.totalSales.toInt()
            )
        }
    }

    @Cacheable(
        cacheNames = ["popularProducts"],
        key = "'cache:product:popular'",
    )
    fun retrievePopularWithCaching(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct>
    = retrievePopular(cmd)

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)
}