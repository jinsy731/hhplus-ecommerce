package kr.hhplus.be.server.lock.executor

interface DistributedLockExecutor {
    fun <T> execute(
        key: String,
        lockType: LockType,
        waitTimeMillis: Long = 1000,
        leaseTimeMillis: Long = 3000,
        block: () -> T
    ): T

    fun <T> executeMulti(
        keys: Array<String>,
        lockType: LockType,
        waitTimeMillis: Long = 1000,
        leaseTimeMillis: Long = 3000,
        block: () -> T
    ): T
}
