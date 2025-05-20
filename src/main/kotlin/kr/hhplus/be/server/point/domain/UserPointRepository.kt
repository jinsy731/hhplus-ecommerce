package kr.hhplus.be.server.point.domain

import kr.hhplus.be.server.point.domain.model.UserPoint

interface UserPointRepository {
    fun getByUserId(userId: Long): UserPoint
    fun save(userPoint: UserPoint): UserPoint
}