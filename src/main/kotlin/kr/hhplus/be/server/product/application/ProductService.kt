package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.application.dto.ProductResult
import kr.hhplus.be.server.product.application.dto.toProductDetail
import kr.hhplus.be.server.product.domain.product.ProductEvent
import kr.hhplus.be.server.product.domain.product.model.Product
import kr.hhplus.be.server.product.domain.product.model.ProductRepository
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository,
    private val eventPublisher: DomainEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable)

        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
        )
    }

    @Cacheable(
        cacheNames = ["productSearch"],
        key = "'cache:product:search:' + (#cmd.keyword != null ? #cmd.keyword : 'all') + ':' + (#cmd.lastId != null ? #cmd.lastId : ${Int.MAX_VALUE}) + ':' + #cmd.pageable.pageSize",
        sync = true
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
    fun validateAndReduceStock(cmd: ProductCommand.ValidateAndReduceStock.Root): Result<Unit> {
        return runCatching {
            val products = productRepository.findAllByIdForUpdate(cmd.items.map { it.productId }.sorted())
            val variants = productRepository.findAllVariantsByIdForUpdate(cmd.items.map { it.variantId }.sorted())

            cmd.items.forEach { item ->
                val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
                val variant = variants.find { it.id == item.variantId } ?: throw ResourceNotFoundException()

                product.validatePurchasability(item.variantId, item.quantity)
                variant.reduceStock(item.quantity)
            }
        }.onSuccess {
            eventPublisher.publish(ProductEvent.StockDeducted(cmd.context))
        }.onFailure { e ->
            eventPublisher.publish(ProductEvent.StockDeductionFailed(cmd.context.copy(failedReason = e.message ?: "Unknown error")))
        }
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
    fun restoreStock(cmd: ProductCommand.RestoreStock.Root) {
        val products = productRepository.findAllByIdForUpdate(cmd.items.map { it.productId }.sorted())
        val variants = productRepository.findAllVariantsByIdForUpdate(cmd.items.map { it.variantId }.sorted())

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            val variant = variants.find { it.id == item.variantId } ?: throw ResourceNotFoundException()

            variant.restoreStock(item.quantity)
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
        sync = true
    )
    fun retrievePopularWithCaching(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct>
    = retrievePopular(cmd)

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)
}