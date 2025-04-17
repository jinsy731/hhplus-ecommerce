package kr.hhplus.be.server.product.infrastructure

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

@Repository
class DefaultProductRepository(private val jpaRepository: ProductJpaRepository): ProductRepository {
    override fun searchByNameContaining(
        keyword: String?,
        pageable: Pageable
    ): Page<ProductListDto> {
        return jpaRepository.findDtoByNameContaining(keyword, pageable)
    }

    override fun findAll(ids: List<Long>): List<Product> {
        return jpaRepository.findAllById(ids)
    }

    override fun save(entity: Product): Product {
        return jpaRepository.save(entity)
    }
}

interface ProductJpaRepository: JpaRepository<Product, Long> {
    @Query("""
        SELECT new kr.hhplus.be.server.product.infrastructure.ProductListDto(
            p.id, p.name, p.basePrice, p.status
        )
        FROM Product p
        WHERE (:name IS NULL OR p.name LIKE %:name%)
    """)
    fun findDtoByNameContaining(
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<ProductListDto>
}

interface ProductVariantJpaRepository: JpaRepository<ProductVariant, Long> {

}

data class ProductListDto(
    val id: Long,
    val name: String,
    val basePrice: BigDecimal,
    val status: ProductStatus
)