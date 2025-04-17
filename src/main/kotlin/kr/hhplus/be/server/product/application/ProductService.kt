package kr.hhplus.be.server.product.application

import kr.hhplus.be.server.common.PageResult
import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.product.domain.product.Product
import kr.hhplus.be.server.product.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(private val productRepository: ProductRepository) {


    fun retrieveList(cmd: ProductCommand.RetrieveList): ProductResult.RetrieveList {
        val productPage = productRepository.searchByNameContaining(cmd.keyword, cmd.pageable) // TODO: pageable 말고 Spring 의존적이지 않은 파라미터를 써야하나 ?
        return ProductResult.RetrieveList(
            products = productPage.content.map { it.toProductDetail() },
            pageResult = PageResult.of(productPage)
        )
    }

    fun findAllById(ids: List<Long>): List<Product> = productRepository.findAll(ids)

    fun validatePurchasability(cmd: ProductCommand.ValidatePurchasability.Root) {
        val products = productRepository.findAll(cmd.items.map { it.productId })

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            product.validatePurchasability(item.variantId, item.quantity)
        }
    }

    @Transactional
    fun reduceStockByPurchase(cmd: ProductCommand.ReduceStockByPurchase.Root) {
        val products = productRepository.findAll(cmd.items.map { it.productId })

        cmd.items.forEach { item ->
            val product = products.find { it.id == item.productId } ?: throw ResourceNotFoundException()
            product.reduceStockByPurchase(item.variantId, item.quantity)
        }
    }
}