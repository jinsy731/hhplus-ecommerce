package kr.hhplus.be.server.user.infrastructure

import kr.hhplus.be.server.user.domain.UserPointHistory
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserPointHistoryRepository: JpaRepository<UserPointHistory, Long> {
    fun findAllByUserId(userId: Long): List<UserPointHistory>
}