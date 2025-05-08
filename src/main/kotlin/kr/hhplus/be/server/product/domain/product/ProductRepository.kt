package kr.hhplus.be.server.product.domain.product

import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(entity: Product): Product

    fun getById(id: Long): Product

    fun searchByKeyword(keyword: String?, lastId: Long?, pageable: Pageable): List<ProductListDto>

    fun searchIdsByKeyword(keyword: String?): List<Long>

    fun findAll(ids: List<Long>): List<Product>

    fun findAllByIdForUpdate(ids: List<Long>): List<Product>
}