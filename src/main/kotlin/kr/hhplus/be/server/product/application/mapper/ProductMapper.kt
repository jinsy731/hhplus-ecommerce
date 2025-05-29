package kr.hhplus.be.server.product.application.mapper

import kr.hhplus.be.server.order.domain.model.OrderItems
import kr.hhplus.be.server.product.application.dto.ProductCommand
import kr.hhplus.be.server.product.application.dto.ProductResult
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.springframework.stereotype.Component

@Component
class ProductMapper {

    fun mapToProductSummary(productDto: ProductListDto): ProductResult.ProductSummary {
        return ProductResult.ProductSummary(
            productId = productDto.id ?: throw IllegalStateException("Product ID is null"),
            name = productDto.name,
            basePrice = productDto.basePrice,
            status = productDto.status,
        )
    }

    fun mapToStockReductionItems(orderItems: OrderItems): List<ProductCommand.ValidateAndReduceStock.Item> {
        return orderItems.asList().map { orderItem ->
            ProductCommand.ValidateAndReduceStock.Item(
                productId = orderItem.productId,
                variantId = orderItem.variantId,
                quantity = orderItem.quantity
            )
        }
    }

    fun mapToStockRestoreItems(orderItems: OrderItems): List<ProductCommand.RestoreStock.Item> {
        return orderItems.asList().map { orderItem ->
            ProductCommand.RestoreStock.Item(
                productId = orderItem.productId,
                variantId = orderItem.variantId,
                quantity = orderItem.quantity
            )
        }
    }
} 