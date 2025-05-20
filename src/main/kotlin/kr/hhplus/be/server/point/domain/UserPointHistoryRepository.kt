package kr.hhplus.be.server.point.domain

import kr.hhplus.be.server.point.domain.model.UserPointHistory

interface UserPointHistoryRepository {
    fun findAllByUserId(userId: Long): List<UserPointHistory>
    fun save(pointHistory: UserPointHistory): UserPointHistory
}