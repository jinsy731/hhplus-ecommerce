package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import kr.hhplus.be.server.product.domain.product.ProductVariant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultProductRepository(private val jpaRepository: ProductJpaRepository): ProductRepository {
    override fun searchByNameContaining(
        keyword: String?,
        pageable: Pageable
    ): Page<Product> {
        return jpaRepository.findByNameContaining(keyword, pageable)
    }

    override fun findAll(ids: List<Long>): List<Product> {
        return jpaRepository.findAllById(ids)
    }

    override fun save(entity: Product): Product {
        return jpaRepository.save(entity)
    }
}

interface ProductJpaRepository: JpaRepository<Product, Long> {
    fun findByNameContaining(name: String?, pageable: Pageable): Page<Product>
}

interface ProductVariantJpaRepository: JpaRepository<ProductVariant, Long> {

}