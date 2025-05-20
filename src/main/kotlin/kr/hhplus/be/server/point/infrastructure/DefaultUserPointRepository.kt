package kr.hhplus.be.server.point.infrastructure

import kr.hhplus.be.server.shared.exception.ResourceNotFoundException
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.point.domain.UserPointRepository
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