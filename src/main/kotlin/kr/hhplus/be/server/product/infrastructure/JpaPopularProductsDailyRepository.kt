package kr.hhplus.be.server.product.infrastructure

import kr.hhplus.be.server.product.domain.stats.PopularProductDailyId
import kr.hhplus.be.server.product.domain.stats.PopularProductsDaily
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPopularProductsDailyRepository : JpaRepository<PopularProductsDaily, PopularProductDailyId> {

}