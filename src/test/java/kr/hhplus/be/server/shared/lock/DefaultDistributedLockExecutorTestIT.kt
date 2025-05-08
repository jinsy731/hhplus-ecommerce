package kr.hhplus.be.server.shared.lock


import kr.hhplus.be.server.lock.executor.DistributedLockExecutor
import kr.hhplus.be.server.lock.executor.LockType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class DefaultDistributedLockExecutorIntegrationTest {

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    lateinit var lockExecutor: DistributedLockExecutor

    @Test
    fun `트랜잭션 커밋 이후에 락이 해제되어야 한다`() {
        val key = "lock:test:tx"
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute {
            lockExecutor.execute(key, LockType.SPIN, 5000, 1000) {
                val locked = redisTemplate.opsForValue().get("lock:$key")
                assertNotNull(locked, "트랜잭션 중에는 락이 있어야 함")
            }

            // 트랜잭션 중: 락 존재해야 함
            assertNotNull(redisTemplate.opsForValue().get("lock:$key"), "트랜잭션 중 락 확인")
        }

        // 트랜잭션 커밋 후: 락이 해제되어야 함
        val valueAfterCommit = redisTemplate.opsForValue().get("lock:$key")
        assertNull(valueAfterCommit, "커밋 후 락이 해제되어야 함")
    }

    @Test
    fun `executeMulti - 트랜잭션 커밋 이후에 멀티 락이 해제되어야 한다`() {
        val keys = arrayOf("lock:test:multi:1", "lock:test:multi:2")
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute {
            lockExecutor.executeMulti(keys, LockType.SPIN, 1000, 5000) {
                // 트랜잭션 중: 모든 키에 대해 락이 있어야 함
                keys.forEach { key ->
                    val value = redisTemplate.opsForValue().get("lock:$key")
                    assertNotNull(value, "트랜잭션 중에는 락이 있어야 함 - $key")
                }
            }

            // 트랜잭션 중: 다시 한 번 락 확인
            keys.forEach { key ->
                val value = redisTemplate.opsForValue().get("lock:$key")
                assertNotNull(value, "트랜잭션 중 락 확인 - $key")
            }
        }

        // 트랜잭션 커밋 후: 모든 키의 락이 해제되어야 함
        keys.forEach { key ->
            val value = redisTemplate.opsForValue().get("lock:$key")
            assertNull(value, "커밋 후 락이 해제되어야 함 - $key")
        }
    }
}