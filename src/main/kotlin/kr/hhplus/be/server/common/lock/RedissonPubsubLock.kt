package kr.hhplus.be.server.common.lock

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedissonPubsubLock(
    private val redissonClient: RedissonClient
) : DistributedLock {

    companion object {
        private const val LOCK_PREFIX = "lock:"
    }

    override fun supports(lockType: LockType): Boolean = lockType == LockType.PUBSUB

    override fun tryLock(key: String, waitTimeMillis: Long, leaseTimeMillis: Long): Boolean {
        val lockKey = "$LOCK_PREFIX$key"
        val lock = redissonClient.getLock(lockKey)

        return lock.tryLock(waitTimeMillis, leaseTimeMillis, TimeUnit.MILLISECONDS)
    }

    override fun unlock(key: String) {
        val lockKey = "$LOCK_PREFIX$key"
        val lock = redissonClient.getLock(lockKey)

        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }

    override fun tryMultiLock(
        keys: Array<String>,
        waitTimeMillis: Long,
        leaseTimeMillis: Long
    ): Boolean {
        val locks = keys.sortedArray()
            .map { key ->
                val lockKey = "$LOCK_PREFIX$key"
                redissonClient.getLock(lockKey) }

        val multiLock = redissonClient.getMultiLock(*(locks.toTypedArray()))
        return multiLock.tryLock(waitTimeMillis, leaseTimeMillis, TimeUnit.MILLISECONDS)
    }

    override fun unlockMulti(keys: Array<String>) {
        val locks = keys.sortedArray()
            .map { key ->
                val lockKey = "$LOCK_PREFIX$key"
                redissonClient.getLock(lockKey) }

        val multiLock = redissonClient.getMultiLock(*(locks.toTypedArray()))
        multiLock.unlock()
    }
}
