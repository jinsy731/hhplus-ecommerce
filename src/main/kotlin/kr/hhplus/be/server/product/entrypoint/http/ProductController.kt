package kr.hhplus.be.server.product.entrypoint.http

import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.product.application.ProductCommand
import kr.hhplus.be.server.product.application.ProductService
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/products")
class ProductController(private val productService: ProductService): ProductApiSpec {

    @GetMapping
    override fun retrieveLists(
        pageable: Pageable,
        @RequestParam keyword: String?,
        @RequestParam lastId: Long?,
    ): ResponseEntity<CommonResponse<ProductResponse.Retrieve.Lists>> {
        val result = productService.retrieveCachedList(ProductCommand.RetrieveList(pageable,lastId, keyword))
        return ResponseEntity.ok(CommonResponse(result.toProductResponse()))
    }

    @GetMapping("/popular")
    override fun retrievePopular(): ResponseEntity<CommonResponse<List<ProductResponse.Retrieve.Popular>>> {
        val fromDate = LocalDate.now().minusDays(3)
        val toDate = LocalDate.now()
        val limit = 5
        
        val cmd = ProductCommand.RetrievePopularProducts(fromDate, toDate, limit)
        val result = productService.retrievePopular(cmd)
        
        return ResponseEntity.ok(CommonResponse(result.map { it.toPopularProductResponse() }))
    }
}