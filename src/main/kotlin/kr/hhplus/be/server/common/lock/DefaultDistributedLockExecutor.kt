package kr.hhplus.be.server.common.lock

import org.springframework.stereotype.Component
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

        return try {
            block()
        } finally {
            lock.unlock(key)
        }
    }
}
