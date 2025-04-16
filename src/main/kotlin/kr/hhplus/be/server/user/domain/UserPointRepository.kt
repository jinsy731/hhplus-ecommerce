package kr.hhplus.be.server.user.domain

interface UserPointRepository {
    fun getByUserId(userId: Long): UserPoint
    fun save(userPoint: UserPoint): UserPoint
}