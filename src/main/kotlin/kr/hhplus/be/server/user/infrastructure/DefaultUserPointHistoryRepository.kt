package kr.hhplus.be.server.user.infrastructure

import kr.hhplus.be.server.user.domain.UserPointHistory
import kr.hhplus.be.server.user.domain.UserPointHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultUserPointHistoryRepository(private val jpaRepository: JpaUserPointHistoryRepository): UserPointHistoryRepository {
    override fun findAllByUserId(userId: Long): List<UserPointHistory> {
        return jpaRepository.findAllByUserId(userId)
    }

    override fun save(pointHistory: UserPointHistory): UserPointHistory {
        return jpaRepository.save(pointHistory)
    }
}