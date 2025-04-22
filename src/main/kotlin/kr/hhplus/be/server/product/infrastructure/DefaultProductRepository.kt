package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductStatus
import kr.hhplus.be.server.product.domain.product.ProductVariant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Repository
class DefaultProductRepository(private val jpaRepository: ProductJpaRepository): ProductRepository {
    override fun searchByNameContaining(
        keyword: String?,
        lastId: Long?,
        pageable: Pageable
    ): List<ProductListDto> {
        return jpaRepository.findByNameWithNoOffset(keyword, lastId,pageable)
    }

    override fun findAll(ids: List<Long>): List<Product> {
        return jpaRepository.findAllById(ids)
    }

    override fun save(entity: Product): Product {
        return jpaRepository.save(entity)
    }

    override fun getById(id: Long): Product {
        return jpaRepository.findById(id).getOrNull() ?: throw ResourceNotFoundException()
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
}

interface ProductVariantJpaRepository: JpaRepository<ProductVariant, Long> {

}

data class ProductListDto(
    val id: Long,
    val name: String,
    val basePrice: Money,
    val status: ProductStatus
)