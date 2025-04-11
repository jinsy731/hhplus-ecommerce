package kr.hhplus.be.server.user.domain

interface UserPointHistoryRepository {
    fun findAllByUserId(userId: Long): List<UserPointHistory>
    fun save(pointHistory: UserPointHistory): UserPointHistory
}