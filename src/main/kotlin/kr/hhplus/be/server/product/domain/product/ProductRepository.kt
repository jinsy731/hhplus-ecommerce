package kr.hhplus.be.server.product.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductRepository {

    fun findByStatus(status: ProductStatus, pageable: Pageable): Page<Product>

    fun searchByNameContaining(keyword: String, pageable: Pageable): Page<Product>

    fun findAll(ids: List<Long>): List<Product>

    fun findByIdWithOptionSpecs(productId: Long): Product?
}