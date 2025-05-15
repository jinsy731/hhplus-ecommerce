package kr.hhplus.be.server.rank

import kr.hhplus.be.server.shared.web.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/ranking")
class RankingController(private val rankingService: RankingService) {

    @GetMapping("/products")
    fun retrieveTopProducts(req: RankingRequest.RetrieveTopProducts): ResponseEntity<CommonResponse<RankingResponse.RetrieveTopProducts.Root>> {
        val result = rankingService.retrieveProductRanking(req.toQuery())

        return ResponseEntity.ok(CommonResponse(result.toResponse()))
    }
}

class RankingRequest {
    data class RetrieveTopProducts(val periodType: RankingPeriod)
}

fun RankingRequest.RetrieveTopProducts.toQuery(): RankingQuery.RetrieveProductRanking {
    return RankingQuery.RetrieveProductRanking(periodType)
}

class RankingResponse {
    class RetrieveTopProducts {
        data class Root(
            val topProducts: List<TopProduct>
        )

        data class TopProduct(
            val rank: Int,
            val name: String,
            val productId: Long
        )
    }
}

fun RankingResult.RetrieveProductRanking.Root.toResponse(): RankingResponse.RetrieveTopProducts.Root {
    return RankingResponse.RetrieveTopProducts.Root(
        topProducts = products.mapIndexed { index, product ->
            RankingResponse.RetrieveTopProducts.TopProduct(
                rank = index + 1,
                name = product.name,
                productId = product.productId
            )
        }
    )
}