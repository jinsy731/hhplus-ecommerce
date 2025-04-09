package kr.hhplus.be.server.user.application

import jakarta.transaction.Transactional
import kr.hhplus.be.server.user.domain.UserPointHistory
import kr.hhplus.be.server.user.domain.UserPointHistoryRepository
import kr.hhplus.be.server.user.domain.UserPointRepository
import org.springframework.stereotype.Service

@Service
class UserPointService(
    private val userPointRepository: UserPointRepository,
    private val userPointHistoryRepository: UserPointHistoryRepository) {

    @Transactional
    fun charge(cmd: UserPointCommand.Charge): UserPointResult.Charge {
        val userPoint = userPointRepository.getByUserId(cmd.userId)
        val newUserPoint = userPoint.charge(cmd.amount, cmd.time) // UserPoint의 time과 UserPointHistory의 time이 일치하는지 확인하려면 time의 주입을 어디까지 밀어내야할까?
        val history = UserPointHistory.createChargeHistory(cmd.userId, cmd.amount, cmd.time)

        userPointRepository.save(newUserPoint)
        userPointHistoryRepository.save(history)

        return UserPointResult.Charge(
            pointAfterCharge = newUserPoint.balance,
            updatedAt = newUserPoint.updatedAt
        )
    }

    @Transactional
    fun retrievePoint(cmd: UserPointCommand.Retrieve): UserPointResult.Retrieve {
        val userPoint = userPointRepository.getByUserId(cmd.userId)

        return UserPointResult.Retrieve(
            point = userPoint.balance,
            updatedAt = userPoint.updatedAt
        )
    }
}