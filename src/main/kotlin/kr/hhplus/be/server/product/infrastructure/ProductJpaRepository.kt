package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository: JpaRepository<Product, Long> {
    fun findByNameContaining(name: String, pageable: Pageable): Page<Product>
}