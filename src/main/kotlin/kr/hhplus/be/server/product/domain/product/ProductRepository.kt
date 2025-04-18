package kr.hhplus.be.server.product.domain.product

import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductRepository {
    fun save(entity: Product): Product

    fun searchByNameContaining(keyword: String?, lastId: Long?, pageable: Pageable): List<ProductListDto>

    fun findAll(ids: List<Long>): List<Product>
}