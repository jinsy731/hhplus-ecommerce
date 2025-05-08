package kr.hhplus.be.server.point.domain

interface UserPointRepository {
    fun getByUserId(userId: Long): UserPoint
    fun save(userPoint: UserPoint): UserPoint
}