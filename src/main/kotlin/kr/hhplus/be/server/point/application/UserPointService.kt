package kr.hhplus.be.server.point.application

import kr.hhplus.be.server.lock.annotation.WithDistributedLock
import kr.hhplus.be.server.lock.executor.LockType
import kr.hhplus.be.server.point.domain.UserPointHistoryRepository
import kr.hhplus.be.server.point.domain.UserPointRepository
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.point.domain.model.UserPointHistory
import kr.hhplus.be.server.shared.event.DomainEventPublisher
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserPointService(
    private val userPointRepository: UserPointRepository,
    private val userPointHistoryRepository: UserPointHistoryRepository,
    private val eventPublisher: DomainEventPublisher
    ) {

    @WithDistributedLock(
        key = "'user:point:' + #cmd.userId",
        type = LockType.SPIN
    )
    @Transactional
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 300, multiplier = 1.5)
    )
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


    @WithDistributedLock(
        key = "'user:point:' + #cmd.userId",
        type = LockType.SPIN
    )
    @Transactional
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 300, multiplier = 1.5)
    )
    fun use(cmd: UserPointCommand.Use): Result<UserPoint> {
        return runCatching {
            val userPoint = userPointRepository.getByUserId(cmd.userId)
            userPoint.use(cmd.amount, cmd.now)
            val history = UserPointHistory.createUseHistory(cmd.userId, cmd.amount, cmd.now)

            userPointRepository.save(userPoint)
            userPointHistoryRepository.save(history)
            userPoint
        }
    }

    @WithDistributedLock(
        key = "'user:point:' + #cmd.userId",
        type = LockType.SPIN,
        waitTimeMillis = 5000
    )
    @Transactional
    fun restore(cmd: UserPointCommand.Restore): Result<UserPoint> {
        return runCatching {
            val userPoint = userPointRepository.getByUserId(cmd.userId)
            userPoint.charge(cmd.amount, cmd.now)
            val history = UserPointHistory.createChargeHistory(cmd.userId, cmd.amount, cmd.now)

            userPointRepository.save(userPoint)
            userPointHistoryRepository.save(history)
            userPoint
        }
    }

    @Transactional(readOnly = true)
    fun retrievePoint(cmd: UserPointCommand.Retrieve): UserPointResult.Retrieve {
        val userPoint = userPointRepository.getByUserId(cmd.userId)

        return UserPointResult.Retrieve(
            userId = userPoint.userId,
            point = userPoint.balance,
            updatedAt = userPoint.updatedAt
        )
    }
}