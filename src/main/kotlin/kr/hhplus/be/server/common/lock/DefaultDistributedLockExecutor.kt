package kr.hhplus.be.server.common.lock

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.lang.IllegalArgumentException

@Component
class DefaultDistributedLockExecutor(
    private val distributedLocks: List<DistributedLock>
) : DistributedLockExecutor {

    override fun <T> execute(
        key: String,
        lockType: LockType,
        leaseTimeMillis: Long,
        waitTimeMillis: Long,
        block: () -> T
    ): T {
        val lock = distributedLocks.find { it.supports(lockType) } ?: throw IllegalArgumentException("Not supported LockType: $lockType")

        if (!lock.tryLock(key, waitTimeMillis, leaseTimeMillis)) {
            throw IllegalStateException("Lock acquisition failed: $key")
        }
        var result: T

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // ✅ 트랜잭션이 존재할 경우 → 트랜잭션 생명주기에 따라 unlock
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    lock.unlock(key)
                }

                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        // 커밋되지 않은 경우도 unlock (롤백 포함)
                        lock.unlock(key)
                    }
                }
            })

            // 락은 유지한 채 블록 실행 (unlock은 트랜잭션 종료 후 처리됨)
            result = block()
        } else {
            // ✅ 트랜잭션이 없을 경우 → 즉시 unlock
            result = try {
                block()
            } finally {
                lock.unlock(key)
            }
        }

        return result
    }
}
