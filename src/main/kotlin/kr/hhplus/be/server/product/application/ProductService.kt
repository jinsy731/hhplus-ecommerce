package kr.hhplus.be.server.product.application

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.infrastructure.JpaPopularProductsDailyRepository
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import kotlin.collections.sorted

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val popularProductsDailyRepository: JpaPopularProductsDailyRepository,
    private val stringRedisTemplate: StringRedisTemplate,
    private val productRedisTemplate: RedisTemplate<String, ProductResult.RetrieveList>,
) {
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable)

        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
        )
    }

    fun retrieveListWithPageCache(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val keyword = cmd.keyword ?: "all"
        val pageSize = cmd.pageable.pageSize
        val lastId = cmd.lastId ?: Long.MAX_VALUE
        val cacheKey = "product:search:page:$keyword:$lastId:$pageSize"

        // 캐시 조회
        val cached = productRedisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return cached
        }

        // 캐시 미스 - DB 조회
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable)

        val result = ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
        )

        // 캐시 저장 (페이지 단위)
        productRedisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(10))
        return result
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

    @Cacheable(
        cacheNames = ["popularProducts"],
        key = "'cache:product:popular'"
    )
    fun retrievePopularWithCaching(cmd: ProductCommand.RetrievePopularProducts): List<ProductResult.PopularProduct>
    = retrievePopular(cmd)

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)
}