package kr.hhplus.be.server.user.infrastructure

import kr.hhplus.be.server.common.exception.ResourceNotFoundException
import kr.hhplus.be.server.user.domain.UserPoint
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.springframework.stereotype.Repository

@Repository
class DefaultUserPointRepository(private val jpaRepository: JpaUserPointRepository): UserPointRepository {
    override fun getByUserId(userId: Long): UserPoint {
        return jpaRepository.findByUserId(userId) ?: throw ResourceNotFoundException()
    }

    override fun save(userPoint: UserPoint): UserPoint {
        return jpaRepository.save(userPoint)
    }
}