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
        userPoint.charge(cmd.amount, cmd.now) // UserPoint의 time과 UserPointHistory의 time이 일치하는지 확인하려면 time의 주입을 어디까지 밀어내야할까?
        val history = UserPointHistory.createChargeHistory(cmd.userId, cmd.amount, cmd.now)

        userPointRepository.save(userPoint)
        userPointHistoryRepository.save(history)

        return UserPointResult.Charge(
            userId = userPoint.userId,
            pointAfterCharge = userPoint.balance,
            updatedAt = userPoint.updatedAt
        )
    }

    @Transactional
    fun use(cmd: UserPointCommand.Use) {
        val userPoint = userPointRepository.getByUserId(cmd.userId)
        userPoint.use(cmd.amount, cmd.now)
        val history = UserPointHistory.createUseHistory(cmd.userId, cmd.amount, cmd.now)

        userPointRepository.save(userPoint)
        userPointHistoryRepository.save(history)
    }

    @Transactional
    fun retrievePoint(cmd: UserPointCommand.Retrieve): UserPointResult.Retrieve {
        val userPoint = userPointRepository.getByUserId(cmd.userId)

        return UserPointResult.Retrieve(
            userId = userPoint.userId,
            point = userPoint.balance,
            updatedAt = userPoint.updatedAt
        )
    }
}