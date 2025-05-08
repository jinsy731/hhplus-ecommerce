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
    private val productRedisTemplate: RedisTemplate<String, ProductListDto>,
) {
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val products = productRepository.searchByKeyword(cmd.keyword, cmd.lastId, cmd.pageable)

        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
        )
    }

    fun retrieveCachedList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val keyword = cmd.keyword ?: "all"
        val cacheIdsKey = "product:search:ids:keyword:$keyword"
        val cacheDataKeyPrefix = "product:search:data"
        val pageSize = cmd.pageable.pageSize
        val lastId = cmd.lastId ?: Long.MAX_VALUE

        val allIds = stringRedisTemplate.opsForList().range(cacheIdsKey, 0, -1)
            ?.mapNotNull { it.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: run {
                val ids = productRepository.searchIdsByKeyword(cmd.keyword)
                if (ids.isNotEmpty()) {
                    stringRedisTemplate.opsForList().rightPushAll(cacheIdsKey, *ids.map { it.toString() }.toTypedArray())
                    stringRedisTemplate.expire(cacheIdsKey, Duration.ofMinutes(10))
                }
                ids
            }

        val pagedIds = allIds.filter { it < lastId }.take(pageSize)
        val keys = pagedIds.map { "$cacheDataKeyPrefix:$it" }

        val cachedValues = productRedisTemplate.opsForValue().multiGet(keys) ?: emptyList()
        val cachedMap = pagedIds.zip(cachedValues).filter { (_, value) -> value != null }.associate { it.first to it.second!! }
        val missedIds = pagedIds.filterNot { cachedMap.containsKey(it) }

        val dbFetched = if (missedIds.isNotEmpty()) {
            productRepository.findSummaryByIds(missedIds).also { fetched ->
                fetched.forEach {
                    productRedisTemplate.opsForValue().set("$cacheDataKeyPrefix:${it.id}", it, Duration.ofMinutes(10))
                }
            }
        } else emptyList()

        val products = pagedIds.mapNotNull { id ->
            cachedMap[id] ?: dbFetched.find { it.id == id }
        }

        return ProductResult.RetrieveList(
            products = products.map { it.toProductDetail() }
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