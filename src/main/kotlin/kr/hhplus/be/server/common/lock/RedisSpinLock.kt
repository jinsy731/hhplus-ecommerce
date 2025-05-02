package kr.hhplus.be.server.common.lock

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class RedisSpinLock(
    private val redisTemplate: StringRedisTemplate
) : DistributedLock {

    companion object {
        private const val LOCK_PREFIX = "lock:"
        private const val RETRY_INTERVAL_MS = 100L  // retry polling 간격

        private val RELEASE_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
        """.trimIndent()
    }

    override fun supports(lockType: LockType): Boolean = lockType == LockType.SPIN

    private val lockId = UUID.randomUUID().toString()

    override fun tryLock(key: String, waitTimeMillis: Long, leaseTimeMillis: Long): Boolean {
        val lockKey = "$LOCK_PREFIX$key"
        val endTime = System.currentTimeMillis() + waitTimeMillis

        while (System.currentTimeMillis() < endTime) {
            val acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockId, Duration.ofMillis(leaseTimeMillis))
            if (acquired == true) {
                return true
            }
            Thread.sleep(RETRY_INTERVAL_MS)
        }

        return false
    }

    override fun unlock(key: String) {
        val lockKey = "$LOCK_PREFIX$key"
        val script = DefaultRedisScript(RELEASE_SCRIPT, Long::class.java)

        redisTemplate.execute(
            script,
            listOf(lockKey),
            lockId
        )
    }
}
