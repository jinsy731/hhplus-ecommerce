package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository
) {
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable) // TODO: pageable 말고 Spring 의존적이지 않은 파라미터를 써야하나 ?
        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() },
        )
    }

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
        // 지정된 날짜로부터 랭킹 조회 (fromDate ~ toDate까지)
        val salesAggregates = popularProductsDailyRepository.findAllById(
            (1..cmd.limit).map { PopularProductDailyId(cmd.toDate, it) }
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

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)
}