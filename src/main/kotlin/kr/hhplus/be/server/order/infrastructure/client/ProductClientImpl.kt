package kr.hhplus.be.server.order.infrastructure.client

import kr.hhplus.be.server.order.domain.client.ProductClient
import kr.hhplus.be.server.order.domain.client.ReduceStockRequest
import kr.hhplus.be.server.order.domain.client.ReduceStockResponse
import kr.hhplus.be.server.order.domain.client.RestoreStockRequest
import kr.hhplus.be.server.order.infrastructure.client.mapper.OrderClientMapper
import kr.hhplus.be.server.product.application.port.ProductApplicationService
import org.springframework.stereotype.Component

/**
 * ProductClient의 구현체
 * Order 도메인의 요청을 Product 도메인의 요청으로 변환하여 처리
 */
@Component
class ProductClientImpl(
    private val productService: ProductApplicationService,
    private val orderClientMapper: OrderClientMapper
) : ProductClient {
    
    override fun validateAndReduceStock(request: ReduceStockRequest): Result<ReduceStockResponse> {
        return runCatching {
            val productCommand = orderClientMapper.mapToValidateAndReduceStockCommand(request)
            
            // Product 서비스 호출
            val result = productService.validateAndReduceStock(productCommand)
            
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: RuntimeException("재고 차감 실패")
            }
            
            // Product 도메인의 응답을 Order 도메인의 응답으로 변환
            orderClientMapper.mapToReduceStockResponse(request)
        }
    }
    
    override fun restoreStock(request: RestoreStockRequest): Result<Unit> {
        return runCatching {
            val productCommand = orderClientMapper.mapToRestoreStockCommand(request)
            
            // Product 서비스 호출
            productService.restoreStock(productCommand)
        }
    }
} 