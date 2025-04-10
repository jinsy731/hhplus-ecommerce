package kr.hhplus.be.server.product.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.common.PaginationResult
import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.product.domain.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductService(private val productRepository: ProductRepository) {

    @Transactional
    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val productPage = productRepository.searchByNameContaining(cmd.keyword, cmd.pageable) // TODO: pageable 말고 Spring 의존적이지 않은 파라미터를 써야하나 ?
        return ProductResult.RetrieveList(
            products = productPage.content,
            paginationResult = PaginationResult.of(productPage)
        )
    }

    @Transactional
    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)
}