package kr.hhplus.be.server.product.infrastructure

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.product.domain.product.model.Product
import kr.hhplus.be.server.product.domain.product.model.ProductRepository
import kr.hhplus.be.server.product.domain.product.model.ProductStatus
import kr.hhplus.be.server.product.domain.product.model.ProductVariant
import kr.hhplus.be.server.shared.domain.Money
import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class DefaultProductRepository(
    private val jpaRepository: ProductJpaRepository,
    private val variantRepository: ProductVariantJpaRepository
    ): ProductRepository {
    override fun searchByKeyword(
        keyword: String?,
        lastId: Long?,
        pageable: Pageable
    ): List<ProductListDto> {
        return jpaRepository.findByNameWithNoOffset(keyword, lastId,pageable)
    }

    override fun searchIdsByKeyword(keyword: String?): List<Long> {
        return jpaRepository.findIdsByName(keyword)
    }

    override fun findAll(ids: List<Long>): List<Product> {
        return jpaRepository.findAllById(ids)
    }

    override fun findAllByIdForUpdate(ids: List<Long>): List<Product> {
        return jpaRepository.findAllByIdForUpdate(ids)
    }

    override fun save(entity: Product): Product {
        return jpaRepository.save(entity)
    }

    override fun getById(id: Long): Product {
        return jpaRepository.findById(id).getOrNull() ?: throw ResourceNotFoundException()
    }

    override fun findSummaryByIds(ids: List<Long>): List<ProductListDto> {
        return jpaRepository.findSummaryByIds(ids)
    }

    override fun findAllVariantsByIdForUpdate(ids: List<Long>): List<ProductVariant> {
        return variantRepository.findAllByIdForUpdate(ids)
    }
}

interface ProductJpaRepository: JpaRepository<Product, Long> {
    @Query("""
    SELECT new kr.hhplus.be.server.product.infrastructure.ProductListDto(
        p.id, p.name, p.basePrice, p.status
    )
    FROM Product p
    WHERE (:name IS NULL OR p.name LIKE CONCAT('%', :name, '%'))
      AND (:lastId IS NULL OR p.id < :lastId)
    ORDER BY p.id DESC
""")
    fun findByNameWithNoOffset(
        @Param("name") name: String?,
        @Param("lastId") lastId: Long?,
        pageable: Pageable  // 혹은 그냥 size만 직접 LIMIT 설정
    ): List<ProductListDto>

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.name LIKE %:name%
        ORDER BY p.id DESC
    """)
    fun findIdsByName(@Param("name") name: String?): List<Long>

    @Query("""
        SELECT p
        FROM Product p
        WHERE p.id in :ids
        ORDER BY p.id
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByIdForUpdate(@Param("ids") ids: List<Long>): List<Product>

    @Query("""
        SELECT new kr.hhplus.be.server.product.infrastructure.ProductListDto(
            p.id, p.name, p.basePrice, p.status
        )
        FROM Product p
        WHERE p.id in :ids
    """)
    fun findSummaryByIds(@Param("ids") ids: List<Long>): List<ProductListDto>
}

interface ProductVariantJpaRepository: JpaRepository<ProductVariant, Long> {
    fun findByProductId(productId: Long): ProductVariant?

    @Query("""
        SELECT v
        FROM ProductVariant v
        WHERE v.id in :variantIds
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByIdForUpdate(@Param("variantIds") variantIds: List<Long>): List<ProductVariant>
}

data class ProductListDto(
    val id: Long,
    val name: String,
    val basePrice: Money,
    val status: ProductStatus
)