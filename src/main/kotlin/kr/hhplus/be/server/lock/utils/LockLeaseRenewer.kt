package kr.hhplus.be.server.lock.utils

import kr.hhplus.be.server.lock.executor.LockType
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
class LockLeaseRenewer(
    private val redisTemplate: StringRedisTemplate,
    private val redissonClient: RedissonClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scheduler = Executors.newScheduledThreadPool(4)
    private val registeredTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    // Spin Lock에서 사용할 lockId 저장소 (key → lockId)
    private val spinLockIdMap = ConcurrentHashMap<String, String>()

    fun registerSpinLockId(key: String, lockId: String) {
        spinLockIdMap[key] = lockId
    }

    fun removeSpinLockId(key: String) {
        spinLockIdMap.remove(key)
    }

    fun scheduleRenewal(
        key: String,
        lockType: LockType,
        leaseTimeMillis: Long
    ) {
        val interval = leaseTimeMillis / 2

        if (registeredTasks.containsKey(key)) return

        val task = scheduler.scheduleAtFixedRate({
            try {
                when (lockType) {
                    LockType.SPIN -> {
                        val lockKey = "lock:$key"
                        val expectedLockId = spinLockIdMap[lockKey]
                        val currentVal = redisTemplate.opsForValue().get(lockKey)
                        if (currentVal == expectedLockId) {
                            redisTemplate.expire(lockKey, Duration.ofMillis(leaseTimeMillis))
                        }
                    }
                    LockType.PUBSUB -> {
                        val lock = redissonClient.getLock("lock:$key")
                        lock.lock(leaseTimeMillis, TimeUnit.MILLISECONDS)
                    }
                }
            } catch (e: Exception) {
                logger.error("Lock renewal failed for key=$key: ${e.message}")
            }
        }, interval, interval, TimeUnit.MILLISECONDS)

        registeredTasks[key] = task
    }

    fun cancelRenewal(key: String, lockType: LockType) {
        registeredTasks.remove(key)?.cancel(true)

        if (lockType == LockType.SPIN) {
            removeSpinLockId("lock:$key")
        }
    }
}
