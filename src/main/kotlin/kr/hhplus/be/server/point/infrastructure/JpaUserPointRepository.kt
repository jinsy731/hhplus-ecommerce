package kr.hhplus.be.server.point.infrastructure

import kr.hhplus.be.server.point.domain.UserPoint
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserPointRepository: JpaRepository<UserPoint, Long> {
    fun findByUserId(userId: Long): UserPoint?
}