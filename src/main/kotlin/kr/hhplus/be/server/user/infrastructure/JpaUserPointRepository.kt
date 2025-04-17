package kr.hhplus.be.server.user.infrastructure

import kr.hhplus.be.server.user.domain.UserPoint
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserPointRepository: JpaRepository<UserPoint, Long> {
    fun findByUserId(userId: Long): UserPoint?
}