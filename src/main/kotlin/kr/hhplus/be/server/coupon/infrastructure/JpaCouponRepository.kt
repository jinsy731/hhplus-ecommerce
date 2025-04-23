package kr.hhplus.be.server.coupon.infrastructure

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.coupon.domain.model.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface JpaCouponRepository: JpaRepository<Coupon, Long> {
    @Query("""
        SELECT c
        FROM Coupon c
        WHERE id = :id
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdForUpdate(id: Long): Coupon?
}