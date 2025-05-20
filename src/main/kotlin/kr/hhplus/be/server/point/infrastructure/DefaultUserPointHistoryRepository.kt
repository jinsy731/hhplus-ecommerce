package kr.hhplus.be.server.point.infrastructure

import kr.hhplus.be.server.point.domain.model.UserPointHistory
import kr.hhplus.be.server.point.domain.UserPointHistoryRepository
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